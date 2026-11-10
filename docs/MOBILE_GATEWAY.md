# Conta técnica exclusiva do servidor

`com.confApi.mobile` atende `/api/mobile`. O aplicativo usa sessões individuais; senha e JWT da integração ficam exclusivamente na ConfApi. O login e as telas de SMS e aprovação existentes continuam em uso.

Copie `confapi-mobile.example.json` para um arquivo privado **fora** do projeto/WAR. Configure `CONFIANCA_MOBILE_CONFIG` ou a propriedade JVM `confianca.mobile.config`; o padrão é `${user.home}/.confianca/confapi-mobile.json`. Restrinja leitura ao usuário de execução do serviço. Reinicie a ConfApi após alterar o arquivo. Nunca commite credenciais.

- `enabled=true` habilita o contrato; sem configuração, responde 503.
- `development=true` aceita HTTP somente no ambiente privado local e usa os destinos de `services`. Não configure essa opção no servidor público.
- Produção: `development=false`, HTTPS e `allowedProductionHosts` contendo apenas as autoridades internas aprovadas (host e porta, quando aplicável). Configure os endereços iniciais de Hub e Manager para autenticação e leitura do `properties_app`. Os destinos efetivos vêm das propriedades ativas do banco, incluindo os aliases `app...`.
- O app recebe somente configurações públicas selecionadas. Não recebe URLs internas para encaminhar sua sessão a outros hosts. O endereço inicial HTTPS da ConfApi e links públicos continuam observáveis.
- Tokens de acesso opacos: 15 minutos. Renovação rotativa: limite absoluto de 8 horas. Logout e reutilização de refresh revogam a sessão.
- **Sessões em memória nesta implementação:** reiniciar a instância exige novo login. Para múltiplas instâncias ou continuidade entre reinícios, implementar armazenamento compartilhado de sessão/revogação antes da implantação nessa topologia.
- Código de contato é gerado e validado no servidor. As operações de negócio exigem aparelho autorizado e pertencem a um catálogo fechado. As validações de negócio/permissão dos serviços originais continuam necessárias. Esta alteração não é uma auditoria completa de autorização por reserva/agência de todas as APIs.

Testes isolados: `mvn -Dtest=MobileSessionsTest,MobileRoutesTest,MobileUpstreamTest,MobileApiTest test`. Usam mocks; não enviam mensagens nem alteram o banco.

Publicação exige configurar o servidor, homologar login/SMS/aprovação e publicar a build que usa `/api/mobile`. Não publicar apenas o app antes do backend. As versões antigas já distribuídas exigem planejamento de atualização, rotação da conta técnica e invalidação dos JWTs antigos. A implementação local não troca a senha de produção.

### Identidade individual no Chat Confiança

As rotas `v1/chat-confianca/` preservam a identidade da sessão mobile depois das verificações de aparelho, rota e usuário. O gateway emite uma delegação interna aleatória, de uso único, válida por 30 segundos e vinculada ao método, caminho, consulta e hash do corpo. O filtro do chat consome essa delegação antes do JWT técnico e atribui somente `ROLE_USER`; a autorização de negócio continua verificando usuário e agência. Cabeçalhos de identidade enviados pelo cliente não são encaminhados.

As sessões e delegações são locais à instância. O destino `confapi` do gateway deve apontar para a mesma instância que recebeu a sessão; não use distribuição aleatória entre instâncias nesse encaminhamento interno. Reiniciar a ConfApi exige nova autenticação no app.
