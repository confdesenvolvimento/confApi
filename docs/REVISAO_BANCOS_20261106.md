> Registro histórico de uma etapa anterior. A estrutura atual usa cinco tabelas novas no Manager e reutiliza usuario/dispositivo. Não aplicar as instruções antigas de banco deste documento. Consulte [a redução atual](REDUCAO_TABELAS_MANAGER_20261106.md) e o script manual 011_manager_auth_reduzido.sql.

# Revisão dos bancos e da implementação — 06/11/2026

**Resultado: a estrutura real ainda não está pronta para o novo fluxo. AUTH_V2_ENROLLMENT pertence ao ConfiancaManger e não existe no estado consultado. Não deve ser criada no CSERVICES.** O ALTER isolado enviado anteriormente tinha um pré-requisito ausente; este relatório e os scripts consolidados substituem aquela orientação para o estado encontrado.

## O que foi verificado diretamente

Consultas JDBC em conexões marcadas como somente leitura, confirmadas com transaction_read_only=1. Foram lidos metadados de tabelas, colunas, índices, referências, históricos técnicos de migração e contagens agregadas. Não foram executados CREATE, ALTER, DROP, INSERT, UPDATE ou DELETE nos servidores reais. Nenhuma aplicação foi iniciada, publicada ou reiniciada.

| Servidor | Banco | Estrutura e resultado |
|---|---|---|
| 172.18.2.52:3306, MySQL 8.0.43 | ConfiancaManger | dispositivo existe e já tem as dez novas colunas; nenhuma AUTH_V2_* existe. |
| 172.18.2.107:3306, MySQL 8.0.46 | CSERVICES | TOKEN_EMISSAO existe, mas não tem nenhuma das dez colunas CS3_* nem os quatro índices do novo protocolo. Não há tabelas de autenticação mobile; há históricos Flyway antigos. |

Os dois servidores distinguem maiúsculas de minúsculas nos nomes de tabela (lower_case_table_names=0). Os scripts respeitam dispositivo no Manager e TOKEN_EMISSAO/TOKEN_NUMBER no CSERVICES. Não foi encontrada AUTH_V2_ENROLLMENT em nenhum dos dois bancos consultados.

## Responsabilidades confirmadas no código

App → confApi → Manager → banco ConfiancaManger. V2Database usa exclusivamente ConfMangerDataSource (spring.datasource.first), a mesma conexão do cadastro dispositivo. Store, V2Manager e V2Policy usam essa conexão. O confApi encaminha autenticação e origem de rede ao Manager; App, Payara e Hub não manipulam diretamente essas tabelas.

O cService3 fornece geração de números de quatro dígitos, inclusive de forma independente e sem banco na rota /api/v2/tokens. Seu módulo opcional de persistência e catálogo usa CSERVICES para emissões contextualizadas e matrizes. Ele não contém rotas, persistência ou migrações de autenticação mobile e não executa Flyway. O banco CSERVICES não deve receber AUTH_V2_* para corrigir este erro.

A tabela CSERVICES.DISPOSITIVO é legada e não é a autoridade de dispositivo do App neste desenho. Não a excluir automaticamente enquanto há consumidores antigos. O mesmo vale para LOTE, IP_USUARIO, LOG_ACESSO e as tabelas de histórico Flyway.

O documento inicial sugeria parte do estado mobile no CService, mas essa proposta foi substituída pela orientação posterior do usuário: cService independente para tokens, autenticação e vínculo do App no Manager. Essa separação foi preservada.

## Manager: o que falta

As onze tabelas abaixo são dependências da implementação atual. Nenhuma delas foi criada por esta revisão; não é uma afirmação de que onze seja o menor número possível em qualquer arquitetura.

| Tabela | Uso atual |
|---|---|
| AUTH_V2_ACCOUNT | Política, versão vigente e referência ao dispositivo existente. |
| AUTH_V2_ENROLLMENT | Solicitação de cadastro/troca, aprovação, chave candidata, confirmação e entrega pendente. candidate_metadata pertence aqui. |
| AUTH_V2_CONTACT | Contato verificado e evidência/operador da verificação. |
| AUTH_V2_HISTORY | Histórico e auditoria, incluindo os dados anteriores e posteriores do dispositivo. |
| AUTH_V2_PRIMARY | Referência temporária da autenticação primária do portal, ligada ao cliente e à sessão. |
| AUTH_V2_OUTBOX | Fila transacional de avisos de pendência. |
| AUTH_V2_LIMIT | Limites de tentativas da camada de integração. |
| AUTH_V2_MOBILE_SESSAO | Sessão provisória/autenticada, chave pública, dispositivo e versão. |
| AUTH_V2_MOBILE_REFRESH | Rotação e detecção de reutilização do refresh. |
| AUTH_V2_MOBILE_DESAFIO | Desafios de portal, plantão e antifraude, com tentativas/consumo. |
| AUTH_V2_MOBILE_CONTROLE | Limites e bloqueio de repetição de provas. |

As tabelas de histórico e contato encontradas no banco pertencem a operações comerciais ou contatos de unidade; não oferecem o contrato e os vínculos exigidos pelo novo código. Reaproveitar uma delas sem alterar a implementação não resolveria a ausência dessas dependências. usuario e dispositivo já são reutilizadas; a identidade do usuário não está sendo recadastrada em outro banco.

**Não basta aplicar apenas candidate_metadata.** O script consolidado cria AUTH_V2_ENROLLMENT já com todas as colunas finais e cria as outras dependências. Não preencher as tabelas com aprovações fictícias: contas surgem no fluxo de login e o dispositivo precisa da aprovação/prova previstas. A ausência de AUTH_V2_ACCOUNT também afeta verificações de V2Policy chamadas por rotas legadas; desligar só a flag mobile não elimina essa dependência. Preparar o schema antes de usar a nova versão do Manager.

As dez colunas novas em dispositivo já estão presentes, com os tipos pedidos. Data de inclusão, data do vínculo, IP e impressão digital ainda estão vazios em todos os registros consultados; não foram preenchidos retroativamente. O script consolidado não tenta acrescentá-las novamente.

Também faltava adequar capacidades: login tem 50 caracteres e o contrato aceita 128; modelo/marca têm 50 e o contrato aceita 100; token_expo tem 255 e o contrato aceita 512. O script amplia essas colunas, permite utf8mb4 nos dados descritivos e acrescenta índice não único em login para a busca/ativação. Não cria restrição única que apagaria ou esconderia duplicidades existentes.

## CSERVICES: o que falta e o que foi corrigido

O script desse banco não cria tabelas. Acrescenta dez colunas CS3_* e quatro índices à TOKEN_EMISSAO, preservando as emissões antigas com CS3_ID nulo. Usa comparação binária nos identificadores novos para não confundir referências opacas que diferem em caixa.

O catálogo também precisava caber no schema real: USUARIO.LOGIN_USUARIO e NOME_USUARIO tinham 45 caracteres; SENHA_USUARIO tinha 45, insuficiente para o hash BCrypt de 60 caracteres. O script amplia os dois primeiros para 100 e a senha para 255, preservando NOT NULL. Amplia ainda cidade e telefones de EMPRESA para os 100 caracteres aceitos pelo contrato.

No código foi corrigida a edição de TOKEN_NUMBER: a posição pertence ao par matriz/posição, não é um identificador global. O banco tem 1.953.575 linhas e 50 posições distintas. Não há duplicidade do par (CODG_TOKEN,CODG_TOKEN_NUMBER) no snapshot. O script cria um índice único composto nesse par, nunca só em CODG_TOKEN_NUMBER. O código agora permite a mesma posição em matrizes diferentes e recusa duplicidade interna, sem alterar os quatro dígitos.

Foi ajustada também a validação de criação de usuário técnico no catálogo: email, telefone e tipo são obrigatórios porque o banco os define como NOT NULL. Campo ausente resulta em erro 400 antes do INSERT, em vez de erro SQL. Atualizações podem continuar omitindo os campos que não mudaram.

As restrições e larguras dos testes sintéticos anteriores não representavam completamente o banco real; por isso aqueles testes aprovados não demonstravam compatibilidade de schema. A massa de teste foi corrigida para posições locais à matriz e campos obrigatórios reais.

## Dados legados que exigem decisão humana

- Manager: 10.787 registros em dispositivo; um grupo de login ativo aparece mais de uma vez. O cadastro/ativação desse usuário pode falhar até o responsável definir qual vínculo é correto. A consulta de diagnóstico mostra login e IDs, sem apagar nada.
- Manager: 1.248 registros de dispositivo sem usuário correspondente; 1.939 sem código de aparelho; um sem login. São contagens de legado, não uma autorização para excluir linhas. A ausência de código antigo, por si só, não autoriza ativar uma chave nova.
- CSERVICES: 35 valores de matriz fora do formato de quatro dígitos. Nenhum estava em matriz ativa e não vencida no momento da consulta. A consulta entrega os IDs e posições, sem exibir os códigos. Não foram completados zeros, trocados valores ou removidos registros.

O histórico flyway_auth_mobile_v2_history registra a migração antiga V1__auth_mobile_v2_shared_mysql.sql em 05/11/2026. Isso registra uma execução passada, não comprova a existência atual das tabelas. A aplicação atual não depende desse histórico. Nenhuma tabela de histórico foi removida ou modificada.

## Scripts para execução manual

Pasta: D:/Projetos/cServices3.0/docs/sql/revisao_20261106.

1. No servidor 172.18.2.52, conferir com 000_conferir_manager.sql (somente leitura) e validar 010_manager_auth_completo.sql em homologação. Ele cria as onze tabelas e amplia campos existentes; inclui candidate_metadata desde a criação.
2. No servidor 172.18.2.107, conferir com 001_conferir_cservices.sql (somente leitura) e validar 020_cservices_tokens.sql em homologação. Ele altera tabelas existentes e cria os índices necessários, sem tabela mobile.
3. Resolver o login duplicado do Manager com decisão do responsável. Reavaliar o diagnóstico após qualquer modificação concorrente.
4. Aplicar manualmente os scripts autorizados no destino correto e depois repetir as consultas. Os scripts são específicos para o estado observado, não são idempotentes e não devem ser executados junto com as migrações antigas 001/002/003/004. Interromper no primeiro erro; não tratar ROLLBACK como reversão de DDL MySQL. O índice da matriz percorre quase dois milhões de linhas, portanto prever janela de manutenção.
5. Publicar os pacotes correspondentes e homologar os fluxos por HTTPS com as chaves/configurações já documentadas. Criação das tabelas não provisiona chaves, entrega de e-mail nem valida a build nativa do App. Nenhuma publicação foi feita nesta revisão.

Os comandos de escrita estão qualificados com o nome do banco. Não há CREATE DATABASE, DROP, DELETE, limpeza automática, cópia entre bancos nem importação de contas aprovadas. Os bancos ainda permanecem no estado lido até a execução manual do responsável.

## Validação e limites

33 testes do cService3 aprovados, com novo JAR gerado. Mais 15 verificações em H2 descartável cobriram estrutura consolidada equivalente às migrações 001–004, chaves estrangeiras, larguras, preservação de linhas antigas e unicidade composta da matriz.

H2 não certifica collation, comportamento de locks nem execução de DDL no MySQL: opções específicas foram removidas somente no adaptador de teste. O MySQL local não estava disponível; não se testou DDL em produção. É necessária homologação dos scripts numa cópia MySQL autorizada antes da aplicação definitiva.

Esta revisão cobre as estruturas de autenticação, dispositivo e tokens relacionadas ao pedido, não uma auditoria integral das 207 tabelas comerciais do Manager. Permanecem as pendências de APIs de negócio do App, uploads/filtros, automações legadas e infraestrutura documentadas em REVISAO_INTEGRACAO_20261106.md. Não está liberada a descontinuação do serviço antigo.

Snapshots de metadados, contagens agregadas, backups e logs: D:/Projetos/cServices3.0/.local/schema-review. Nenhuma credencial foi colocada nos scripts ou neste relatório.
