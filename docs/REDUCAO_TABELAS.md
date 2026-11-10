# Redução das tabelas de autenticação — 06/11/2026

O Manager foi ajustado de onze para **cinco tabelas novas**, todas em minúsculas, reutilizando `usuario` e `dispositivo`. O usuário confirmou que não executou a criação das onze tabelas anteriores. Por isso, esta entrega prepara a criação direta da estrutura reduzida, sem copiar ou excluir dados de `AUTH_V2_*`.

**Nenhuma alteração foi executada nos bancos reais.** Não houve deploy, reinício de serviço, envio de mensagens nem novo acesso ao banco nesta etapa. A referência de estrutura é a consulta somente leitura da revisão anterior, junto da confirmação do usuário.

## Finalidade de cada tabela anterior e destino

| Tabela anterior | Para que servia | Solução atual |
| --- | --- | --- |
| AUTH_V2_ACCOUNT | Versão de segurança do usuário e indicação do aparelho aprovado. | Campos em `usuario`; chave e dados do aparelho em `dispositivo`. Não criar outra conta. |
| AUTH_V2_ENROLLMENT | Solicitação de cadastro/troca, candidato, aprovação, prazo, confirmação e idempotência. | `dispositivo_solicitacao`. |
| AUTH_V2_CONTACT | E-mail validado independentemente, evidência, operador e data. | Campos em `usuario`, sem tabela própria. |
| AUTH_V2_HISTORY | Histórico de aprovação/troca e eventos de autenticação, com dados anteriores/posteriores. | `dispositivo_historico`. |
| AUTH_V2_PRIMARY | Referência temporária após login/senha no portal, ainda sem concluir MFA. | `autenticacao_sessao`, tipo PORTAL. |
| AUTH_V2_OUTBOX | Aviso de nova pendência à TI, com estado de envio e nova tentativa após falha. | Colunas na própria `dispositivo_solicitacao`, gravadas na mesma transação. |
| AUTH_V2_LIMIT | Contadores de tentativas das integrações. | `autenticacao_controle`, tipo LIMITE. |
| AUTH_V2_MOBILE_SESSAO | Sessão provisória/autenticada do App, chave, aparelho, versão e vencimento. | `autenticacao_sessao`, tipo MOBILE. |
| AUTH_V2_MOBILE_REFRESH | Renovação de sessão, rotação do token e detecção de reutilização. | `autenticacao_controle`, tipo REFRESH. |
| AUTH_V2_MOBILE_DESAFIO | Tentativa específica de login, plantão ou antifraude; prazo, código protegido, tentativas e consumo. | `autenticacao_desafio`. |
| AUTH_V2_MOBILE_CONTROLE | Limites compartilhados e registro de provas já usadas para impedir repetição. | `autenticacao_controle`, tipos LIMITE/REPLAY. |

Portanto, ACCOUNT, CONTACT, PRIMARY, OUTBOX, LIMIT e MOBILE_REFRESH deixam de exigir tabelas separadas. As funções de segurança continuam implementadas nas estruturas indicadas. As cinco restantes são solicitações, histórico, sessões, desafios e controles temporários; separá-las evita misturar auditoria durável com registros que vencem e são limpos. Este desenho é uma redução concreta, não uma afirmação de mínimo matemático para qualquer arquitetura.

## Dados nas tabelas existentes

`dispositivo` continua sendo o cadastro do aparelho. A ativação aprovada conserva seu `codg_dispositivo` e registra chave pública, data do vínculo, IP observado pelo backend e dados descritivos. As dez colunas de metadados já estavam presentes no banco conferido. Datas antigas desconhecidas permanecem nulas. Nenhuma aprovação é gerada pelo script.

Oito colunas são acrescentadas a `usuario`: `versao_minima_autenticacao`, `versao_dispositivo`, `codg_dispositivo_autenticacao`, `email_autenticacao`, `contato_autenticacao_ref`, `contato_autenticacao_evidencia`, `contato_autenticacao_operador` e `contato_autenticacao_verificado_em`. Elas ficam sob controle do módulo de autenticação; não foram expostas nem incluídas nos mapeamentos de gravação da entidade legada `Usuario`.

O contato verificado é usado quando o suporte confirma um destino de e-mail para enviar a confirmação do cadastro ou uma operação de plantão pelo canal EMAIL. O endereço comum do cadastro não comprova essa verificação. São mantidos o endereço validado, quem o validou, quando e a evidência. Alterá-lo gera uma nova referência e histórico; uma entrega pendente com a referência anterior é recusada. Verificar contato não aprova aparelho. O canal APP e a aprovação pela TI permanecem sujeitos às suas próprias regras.

Os metadados do aparelho candidato ficam em `dispositivo_solicitacao.candidate_metadata` até a ativação. **Não executar o ALTER isolado de AUTH_V2_ENROLLMENT**: o novo script cria a coluna diretamente na tabela correta.

## Aplicações e banco

As cinco tabelas novas e os campos adicionais ficam exclusivamente no **`ConfiancaManger`**, servidor `172.18.2.52:3306`, ligado ao `ConfMangerDataSource`. Apenas o Manager as consulta e grava. App → confApi → Manager continua sendo o caminho de autenticação do aplicativo. Hub/Payara consomem os serviços do Manager pelas integrações existentes.

O cService3 continua independente, gerando os tokens e mantendo seu catálogo/ciclo próprio quando habilitado. **Nenhuma dessas tabelas pertence ao CSERVICES.** Não foi alterada a rotina de quatro dígitos (geração dinâmica de 1000 a 9999). O ajuste anterior de colunas de TOKEN_EMISSAO e catálogo do cService é outra atividade e permanece no script 020, sem criar tabelas mobile.

As rotas e os formatos de resposta foram preservados. Por isso, confApi, Hub, Payara e App não precisam de alteração de código por causa desta redução interna. A documentação compartilhada foi atualizada. Esta revisão não conclui as pendências anteriores das APIs de negócio do App, allowlists, configuração HTTPS, entrega real de e-mail ou build nativa.

## Script manual e ordem

1. Conferir o estado atual com `docs/sql/revisao_20261106/000_conferir_manager.sql` (somente leitura). Resolver com o responsável eventuais logins/dispositivos duplicados; o código recusa vínculos ambíguos e o script não apaga registros.
2. Validar **`docs/sql/revisao_20261106/011_manager_auth_reduzido.sql`** numa cópia MySQL do Manager e executar manualmente no destino correto. Ele amplia campos de `dispositivo`, acrescenta oito campos a `usuario`, índices e cinco tabelas. Não recria as dez colunas de metadados existentes, não altera dados de usuários nem contém DROP/DELETE.
3. Publicar o novo WAR do Manager somente depois de preparar o schema. As verificações de política também são usadas por rotas legadas; desligar somente a flag mobile não dispensa os novos campos. Homologar aprovação, troca, login, revogação, prazo e reenvio de avisos com as demais aplicações.

O script é específico para a estrutura conferida e **não deve ser repetido após aplicação**. Interromper no primeiro erro; MySQL DDL não é desfeito por ROLLBACK. Se houver aplicação parcial ou alterações desde a consulta, conferir antes de continuar. Os antigos scripts 001–004 do módulo e o 010 de onze tabelas foram substituídos por avisos sem DDL. O recurso `005_autenticacao_reduzida.sql` descreve o esquema usado nos testes; não é uma segunda etapa para executar junto do 011 e não é carregado automaticamente.

## Validação

79 testes isolados de autenticação do Manager aprovados, incluindo nove novos casos sobre a redução: referências do portal versus tokens mobile, tipos de controle sem colisão, manutenção da detecção de reutilização do refresh, rejeição de registros com tipos misturados, reenvio transacional de avisos, contato não verificado/referência antiga, login duplicado e divergência da chave aprovada. Os testes anteriores continuam cobrindo quatro dígitos, concorrência de ativação, revogação, limites, expiração, prova da chave e rollback da auditoria.

Mais 12 verificações isoladas confirmaram que o script manual corresponde ao recurso do Manager, cria somente cinco tabelas, preserva usuário/aparelho antigos, não aprova ninguém automaticamente e aplica referências e restrições. H2 é descartável e usa dados sintéticos. Ele não certifica collation, locks ou DDL específico do MySQL; nenhuma escrita foi feita em produção.

O H2 foi atualizado somente no escopo de testes para 2.3.232, já disponível no ambiente, para suportar a sintaxe SKIP LOCKED usada pelos avisos. O WAR final, seu hash e a origem dos resultados dos demais projetos estão em `PACOTES_SERVIDORES_V2.json`. Não foram repetidos os testes dos consumidores sem mudança de código. Logs e backups desta etapa: `D:/Projetos/cServices3.0/.local/reduce-auth`.
