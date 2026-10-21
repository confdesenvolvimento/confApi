# Campo valor cifrado no CService

O GET `/api/token/gerartoken/{chave}/{login}` retorna `valor` como Base64 de
`IV de 12 bytes + ciphertext + tag GCM de 128 bits`. A chave AES-256 e o SHA-256
do segredo UTF-8 configurado no CService em `cservices.token.hash-secret`.

O ConfApi usa `cservices.token.hash-secret` ou `CSERVICES_TOKEN_HASH_SECRET`,
que deve ter o mesmo valor configurado no CService. Na ausencia de configuracao,
ambos mantem o fallback de desenvolvimento `cServices2-token-hash-dev`.
Nenhuma chave deve ser enviada ao navegador ou colocada neste documento.

No canal EMAIL do plantao, o ConfApi descriptografa somente para montar o SMS
enviado ao Hub. A resposta `PlantaoTokenResponse.token` preserva o valor recebido
do CService. No canal APP, esse valor continua sendo apenas repassado. O Payara
usa sua presenca para habilitar a etapa de digitacao, sem comparar o ciphertext
com o codigo informado pelo usuario.

Para permitir atualizar os servicos em qualquer ordem, valores antigos contendo
exatamente quatro digitos tambem sao aceitos para envio por SMS. Valores cifrados
invalidos, adulterados ou com chave incorreta fazem a geracao falhar antes do SMS.
A validacao continua recebendo o codigo numerico digitado pelo usuario.

O login Payara via SOAP/Hub, a consulta de cache e a validacao do Manager por hash
no banco nao precisam de alteracao para esta mudanca nas respostas REST.

Teste sem rede, banco ou SMS real:

```text
mvn -Dtest=PlantaoTokenEncryptedResponseTest test
```
