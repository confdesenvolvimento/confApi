# Configuração do cService3 e integrações

Não importar a antiga configuração privada de datasource. Credenciais, chaves e configurações de produção não foram alteradas.

| Variável do cService3 | Uso |
| --- | --- |
| SERVER_PORT | 8087 por padrão. Configurar TLS nativo/terminação com conexão HTTPS até o serviço. Não confiar em X-Forwarded-* arbitrário. |
| CSERVICE_TOKENS_ENABLED | false por padrão; habilita as rotas protegidas. |
| CSERVICE_TOKEN_ISSUER | Emissor técnico confiável. |
| CSERVICE_TOKEN_PUBLIC_JWKS | Somente chaves públicas RSA >=2048 bits, com kid. |
| CSERVICE_TOKEN_CLIENTS | IDs separados por vírgulas; exemplo manager,hub,integracao-independente. |
| CSERVICE_STORAGE_ENABLED | false por padrão. Habilitar somente depois da preparação autorizada do schema de tokens. |
| CSERVICE_DB_URL / CSERVICE_DB_USER / CSERVICE_DB_PASSWORD | Banco CSERVICES exclusivo do serviço de tokens. Nenhuma URL privada padrão. Usar UTC no servidor/JVM/conexão. |
| CSERVICE_CLIENT_POLICIES_JSON | Política explícita client → companyId, actorUserId, purposes e matrix; exemplo abaixo. |
| CSERVICE_STORAGE_KEYS_JSON | Chave ativa e chaveiro com HMAC >=32 bytes e AES de 32 bytes, em Base64, distintas. Manter chaves anteriores até expirar/revogar suas emissões. |
| CSERVICE_TOKEN_CLEANUP_ENABLED | false por padrão. Se persistência estiver habilitada, limpa códigos cifrados expirados a cada minuto, em lotes de 100. Não apaga o histórico. |

Exemplo de política (IDs sintéticos; substituir pelos cadastros autorizados existentes):

```json
{"hub":{"companyId":1,"actorUserId":1,"purposes":["LEGACY_MATRIX"],"matrix":true},"integracao-independente":{"companyId":2,"actorUserId":2,"purposes":["CONFIRMACAO_OPERACAO"],"matrix":false}}
```

O Manager usa somente token:generate e não precisa de política de persistência. AUTH_CONFIRMATION, PORTAL_LOGIN, MOBILE_LOGIN e DEVICE_ENROLLMENT não são finalidades aceitas no armazenamento do cService.

Formato do chaveiro: {"active":"k1","keys":{"k1":{"hmac":"BASE64_HMAC","encryption":"BASE64_AES"}}}. Não usar os placeholders como chaves. Não copiar senhas/chaves do serviço antigo.

JWT técnico: RS256; typ at+jwt; token_use SERVICE; sub igual ao cliente; iss configurado; aud exatamente cservice3-tokens; iat e exp, validade máxima de 300 segundos. O emissor deve renovar o arquivo de credencial antes de expirar. O cService recebe só chaves públicas e não oferece login/senha para criar credenciais técnicas.

| Operação | Escopo |
| --- | --- |
| Gerar número | token:generate |
| Emitir/validar/consultar/revogar | token:issue / token:validate / token:status / token:revoke |
| Desafio físico | matrix:challenge + política matrix=true |
| Ler/manter catálogo | catalog:read / catalog:write |
| Ler valores da matriz para administração | catalog:read + matrix:read + política matrix=true |
| Criar empresa | catalog:write + catalog:provision; nova empresa exige nova política administrativa |
| Consultar cartão bloqueado | card:check |

O Hub usa CSERVICE_TOKEN_URL (base HTTPS, sem barra final) e CSERVICE_TOKEN_CLIENT_FILE com matrix:challenge, token:validate e token:revoke. A identidade deve ter a finalidade LEGACY_MATRIX. A política do usuário é consultada no Manager na criação e na validação, sem fallback em indisponibilidade.

Manager → cService continua usando CSERVICE_TOKEN_URL e CSERVICE_TOKEN_CLIENT_FILE com sub=manager e scope=token:generate. A autenticação mobile mantém AUTH_MOBILE_ENABLED, AUTH_V2_ENABLED, chaves próprias, URL pública do confApi e as configurações de sessão/recursos anteriores.

Para operações, configurar no Manager AUTH_V2_OPERATION_CLIENTS_JSON={"confapi":["PLANTAO"],"hub":["ANTIFRAUDE"]}. Somente os serviços explicitamente listados podem iniciar/validar esses propósitos. As credenciais AUTH_V2_MANAGER_TOKEN_FILE de confApi e Hub precisam de aud=manager-v2, token_use=SERVICE e escopos v2:access operation:create operation:validate, além dos escopos de suas outras funções. O contrato interno é POST /internal/v2/operacoes/desafios e POST /internal/v2/operacoes/desafios/{id}/validar.

Payara → confApi plantão usa AUTH_V2_CONFAPI_LEGACY_TOKEN_FILE (aud confapi-v2, scope legacy:token); Payara → Hub matriz física usa AUTH_V2_HUB_LEGACY_TOKEN_FILE (aud hub-v2, scope legacy:token). São nomes mantidos dos arquivos de integração; o transporte já é REST. AUTH_V2_ENABLED precisa estar habilitado nos gateways. O App nunca recebe essas credenciais.

Entrega de plantão por e-mail usa AUTH_V2_MAIL_URL e AUTH_V2_MAIL_TOKEN_FILE existentes. Destino é exclusivamente o contato de autenticação verificado em usuario, com evidência e operador. O serviço de entrega deve aceitar propósito PLANTAO e Idempotency-Key. Falha não libera acesso nem troca de canal. SMS e contratos externos antigos de login+código precisam de migração própria.
