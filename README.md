# ConfAPI

## Execução local

O projeto utiliza Java 18. No IntelliJ, selecione o Amazon Corretto 18 para o
Project SDK e para a configuração de execução da aplicação.

Os perfis `dev` e `prod` possuem parâmetros fixos para a integração do Minhas
Viagens. O `application-dev.properties` aponta para o mViagensBackend local na
porta `8091` e para o Manager local na porta `8082`.

Em desenvolvimento, use somente o perfil `dev`. O token local está configurado
no `application-dev.properties` para este ambiente. Não reutilize esse valor em
produção e não faça commit/push desse segredo. O perfil `dev-local` não é necessário.

O `mViagensBackend` deve estar iniciado antes do ConfAPI. O perfil `dev` usa o
backend local na porta `8091` e o Manager local na porta `8082`.

Validação:

```bash
curl http://127.0.0.1:8091/actuator/health/readiness
```

O endpoint público de cadastro do cliente final fica em:

```text
POST http://127.0.0.1:8088/api/client-app/v1/public/auth/cpf/flows
```
# Fronteira do Minhas Viagens

O fluxo B2C deve respeitar esta topologia:

```text
Aplicativo Minhas Viagens ─┐
                           ├──> ConfAPI ───> mViagensBackend ───> mViagensApp
FrontendPayara ────────────┘        ├──────> Manager
                                   ├──────> Hub / pagamentos / fornecedores
                                   └──────> appConf
```

O aplicativo e o FrontendPayara nunca chamam o `mViagensBackend` diretamente.
As chamadas para o mViagensBackend usam apenas a integração interna do ConfAPI,
com `X-MViagens-Service-Token`. O mViagensBackend não consulta Manager, Hub ou
appConf.

No login por CPF, o ConfAPI consulta os contatos no Manager e encaminha ao
mViagensBackend somente os dados mínimos do candidato. O mViagensBackend cria,
persiste, envia e valida o OTP por SMTP; o ConfAPI devolve ao cliente somente o
destino mascarado. O código temporário não faz parte de nenhuma resposta HTTP.
