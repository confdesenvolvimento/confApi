> Registro histórico de uma etapa anterior. A estrutura atual usa cinco tabelas novas no Manager e reutiliza usuario/dispositivo. Não aplicar as instruções antigas de banco deste documento. Consulte [a redução atual](REDUCAO_TABELAS_MANAGER_20261106.md) e o script manual 011_manager_auth_reduzido.sql.

# Dados do dispositivo — 06/11/2026

Implementado no código e validado com dados sintéticos. Nenhuma conexão ou alteração no banco de produção; nenhum deploy. O SQL deve ser executado manualmente pelo responsável, conforme solicitado.

## Script manual

Arquivo no Manager: src/main/resources/db/auth-v2/004_dispositivo_metadados.sql. Cópia para entrega no cService3: docs/sql/004_dispositivo_metadados_manager.sql. Apesar da localização da cópia, **o destino é exclusivamente o banco usado pelo Manager, nunca CSERVICES**. O arquivo não contém USE: selecione explicitamente o banco correto antes da execução.

O script acrescenta dez colunas em dispositivo e uma em AUTH_V2_ENROLLMENT. Não cria tabelas, não apaga linhas e não preenche registros históricos. Pressupõe as estruturas das migrações 001, 002 e 003 já disponíveis. Como o schema real não foi consultado, confira as colunas existentes antes de executar; os ALTER TABLE não devem ser repetidos se já foram aplicados. MySQL pode confirmar DDL independentemente de transações: não tratar ROLLBACK como reversão desses comandos.

| Coluna em dispositivo | Origem e significado |
|---|---|
| data_inclusao | Horário do servidor ao criar o registro; preservado nas trocas. |
| data_vinculo | Horário da ativação aprovada do aparelho atualmente vinculado. |
| ip_cadastro | Endereço IPv4/IPv6 observado pelo confApi ao iniciar a solicitação daquele aparelho. Não representa o IP de cada acesso posterior. |
| chave_publica_thumbprint | Impressão digital da chave pública comprovada pela sessão; identifica a instalação aprovada. |
| fabricante_dispositivo | Fabricante informado pelo sistema do aparelho. |
| identificador_modelo | Código do modelo, quando disponível; não é um número de série individual. |
| sistema_operacional | Nome do sistema, por exemplo Android ou iOS. |
| versao_sistema | Versão do sistema no momento da solicitação. |
| versao_app | Versão do aplicativo nativo instalado. |
| build_app | Número da compilação nativa instalada. |

Os campos codigo_dispositivo, modelo_dispositivo e marca_dispositivo já existentes continuam em uso. AUTH_V2_ENROLLMENT.candidate_metadata armazena os dados descritivos e o IP do candidato enquanto a solicitação aguarda decisão; o histórico continua em AUTH_V2_HISTORY.device_before_json/device_after_json. Nenhuma tabela foi acrescentada nesta alteração.

## Fluxo e proteção

App → confApi → Manager → banco do Manager. O App envia somente modelo, marca, fabricante, identificador de modelo, sistema e versões. O confApi gera X-Auth-Client-IP a partir de getRemoteAddr(), sem copiar os cabeçalhos de IP enviados pelo cliente. O Manager exige a identidade técnica autorizada do confApi antes de aceitar o endereço, valida o literal IPv4/IPv6 e não faz resolução DNS. Data, usuário, aprovação e chave de identidade não são determinados pelo corpo de metadados.

Se houver proxy reverso, getRemoteAddr() pode representar o proxy. Para registrar o endereço original, a configuração de infraestrutura precisa reconhecer somente proxies confiáveis; não aceitar X-Forwarded-For arbitrário. A topologia real não foi testada.

O IP é evidência de rede, não identidade exclusiva do aparelho. A impressão digital identifica a chave daquela instalação, não um identificador físico permanente: uma reinstalação que gere outra chave requer novo vínculo aprovado. Modelo, fabricante e versões são dados descritivos fornecidos pelo cliente e não autorizam acesso. Não houve coleta de IMEI, MAC, número de série ou identificador publicitário.

Enquanto a solicitação está pendente, o dispositivo atual permanece intacto. Após aprovação e ativação, os novos dados entram na mesma linha de dispositivo e os dados anteriores ficam no histórico, na mesma transação. Repetições idempotentes não alteram a origem nem duplicam o histórico. Na troca, data_inclusao é preservada e data_vinculo é renovada.

Registros e solicitações anteriores não recebem dados inventados: data_inclusao e IP desconhecidos permanecem NULL. Novos cadastros feitos pela entidade legada também recebem data de inclusão do servidor; a coleta completa de IP e dados do aparelho pertence ao novo fluxo mobile. Atualizações legadas não podem apagar ou falsificar os novos campos. A consulta antiga do Payara foi adaptada para aceitar os campos adicionais.

## Aplicação manual e publicação

A migração 004 é pré-requisito para esta versão do Manager inclusive com o piloto mobile desabilitado, pois a entidade dispositivo mapeia as novas colunas. Primeiro validar o script numa cópia autorizada de homologação. Para a atualização coordenada, preparar o schema, atualizar a compatibilidade do Payara e o confApi que envia o cabeçalho de IP, depois o Manager e a build nativa do App. O cService e o Hub não precisam de alteração de código para estes dados. Nenhuma dessas publicações foi feita nesta execução.

O App declara expo-application ~57.0.2, já resolvido no lockfile, para ler a versão nativa instalada. A build nativa continua pendente; Expo Go não valida a proteção de chaves deste autenticador.

## Validação desta alteração

- Manager: 70 testes isolados, incluindo H2, autenticação HTTP, idempotência, troca/auditoria, bloqueio de IP inválido e persistência JPA legada.
- confApi: 35 testes isolados; cabeçalhos de IP falsificados não substituem a origem observada.
- Payara: 138 testes isolados; leitura de dispositivo compatível com os novos campos.
- App: 47 testes em quatro suítes shared/authv2; TypeScript validado.
- Total desta execução: 290 testes, todos aprovados. Gerados os WARs de Manager, confApi e Payara. Sem conexão MySQL real, teste integrado por HTTPS, envio de e-mail ou APK/IPA.

Os códigos de verificação continuam com quatro dígitos. Persistem as pendências anteriores de homologação e APIs de negócio descritas em REVISAO_INTEGRACAO_20261106.md. Logs e backups: D:/Projetos/cServices3.0/.local/device-metadata. PACOTES_SERVIDORES_V2.json distingue esta validação das verificações anteriores dos projetos sem mudança.
