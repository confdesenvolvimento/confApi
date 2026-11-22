# Conta técnica exclusiva do servidor

`com.confApi.mobile` atende `/api/mobile`. O aplicativo usa sessões individuais; senha e JWT da integração ficam exclusivamente na ConfApi. O login e as telas de SMS e aprovação existentes continuam em uso.

O pacote de produção pode incorporar `confapi-mobile.json` em `WEB-INF/classes`, sem exigir arquivo externo no servidor. O recurso privado local em `src/main/resources/confapi-mobile.json` é ignorado pelo Git e precisa ser preparado antes do empacotamento. Nunca commite credenciais nem publique o WAR em um local público.

A precedência é: caminho explícito em `confianca.mobile.config`/`CONFIANCA_MOBILE_CONFIG`, recurso incorporado no WAR, arquivo local `${user.home}/.confianca/confapi-mobile.json`. O caminho explícito continua sendo um override opcional. Reinicie/reimplante a aplicação para carregar alterações.

- `enabled=true` habilita o contrato; sem configuração, responde 503.
- `development=true` aceita HTTP somente no ambiente privado local e usa os destinos de `services`. Não configure essa opção no servidor público.
- Produção: `development=false`, HTTPS e `allowedProductionHosts` contendo apenas as autoridades internas aprovadas (host e porta, quando aplicável). Configure os endereços iniciais de Hub e Manager para autenticação e leitura do `properties_app`. Os destinos efetivos vêm das propriedades ativas do banco, incluindo os aliases `app...`.
- Para publicar uma nova versão sem alterar o cadastro compartilhado, `productionServices` pode fixar os destinos dessa implantação (`manager`, `hub`, `confapi`, `cservice` etc.). Cada destino continua exigindo HTTPS e host autorizado. Os serviços não fixados continuam usando o cadastro do Manager.
- O app recebe somente configurações públicas selecionadas. Não recebe URLs internas para encaminhar sua sessão a outros hosts. O endereço inicial HTTPS da ConfApi e links públicos continuam observáveis.
- Tokens de acesso opacos: 15 minutos. Sessão autenticada: limite absoluto de 20 dias. Identificação ainda não autenticada: 30 minutos; após senha/OTP válido, recebe o prazo autenticado. Logout, revogação de aparelho/usuário e reutilização indevida do refresh continuam bloqueando o acesso.
- **Persistência das sessões DPoP aprovadas:** `MobileSessionRepository` usa as tabelas existentes `autenticacao_sessao` e `autenticacao_controle` do Manager. A sessão é restaurada após reinício/entre instâncias, conferindo usuário e versão do aparelho. Sessões Bearer antigas e identificações pendentes continuam transitórias; não são convertidas em sessões autenticadas confiando apenas em um identificador local.
- Código de contato é gerado e validado no servidor. As operações de negócio exigem aparelho autorizado e pertencem a um catálogo fechado. As validações de negócio/permissão dos serviços originais continuam necessárias. Esta alteração não é uma auditoria completa de autorização por reserva/agência de todas as APIs.

Testes isolados: `mvn -Dtest=MobileSessionsTest,MobileRoutesTest,MobileUpstreamTest,MobileApiTest test`. Usam mocks; não enviam mensagens nem alteram o banco.

Publicação exige configurar o servidor, homologar login/SMS/aprovação e publicar a build que usa `/api/mobile`. Não publicar apenas o app antes do backend. As versões antigas já distribuídas exigem planejamento de atualização, rotação da conta técnica e invalidação dos JWTs antigos. A implementação local não troca a senha de produção.

### Identidade individual no Chat Confiança

As rotas `v1/chat-confianca/` preservam a identidade da sessão mobile depois das verificações de aparelho, rota e usuário. O gateway emite uma delegação interna aleatória, de uso único, válida por 30 segundos e vinculada ao método, caminho, consulta e hash do corpo. O filtro do chat consome essa delegação antes do JWT técnico e atribui somente `ROLE_USER`; a autorização de negócio continua verificando usuário e agência. Cabeçalhos de identidade enviados pelo cliente não são encaminhados.

As delegações internas do chat continuam locais à instância. O destino `confapi` do gateway deve apontar para a mesma instância que emitiu essa delegação; não use distribuição aleatória nesse encaminhamento interno. As sessões mobile DPoP aprovadas, por sua vez, são persistidas e podem ser restauradas após reinício.


### Recuperação de renovação interrompida — 18/11/2026

`POST /api/mobile/renovar` aceita `renewalId` opcional, UUID v4 gerado pelo app e salvo no SecureStore antes do envio. Ele identifica uma única rotação. O app mantém esse identificador em falha de rede/gravação e o remove somente ao salvar os novos tokens. Não registrar corpos, refresh, provas ou identificadores de recuperação em logs.

A recuperação exige uma prova DPoP nova do mesmo aparelho a cada envio. O backend deriva sucessores distintos com HMAC-SHA256, usando o refresh de 256 bits e rótulos separados para access/refresh, sessão e identificador da operação. Somente hashes são persistidos. O recibo tem namespace próprio e usa o tipo REPLAY já suportado pela tabela de controles; nenhuma alteração de schema foi adicionada nesta correção.

A transação bloqueia usuário, sessão e refresh na mesma ordem. Uma repetição recupera exatamente os mesmos tokens por até dois minutos, somente enquanto esses sucessores forem os atuais e o novo refresh ainda não tiver sido usado. Reinício e perda de confirmação do commit não criam outra rotação. Outra operação, uma rotação mais antiga ou recibo expirado continuam acionando a proteção contra reutilização. Logout, usuário inativo, aparelho revogado/alterado e expiração absoluta não podem ser desfeitos pela recuperação. Clientes sem DPoP mantêm a regra estrita anterior.

O app continua com uma única renovação em andamento. Faz no máximo uma repetição imediata em falhas transitórias dessa operação vinculada ao aparelho; cada envio cria outro `jti`. Não há repetição automática de emissões, pagamentos ou outros POSTs de negócio. As consultas GET de perfil/dispositivo podem renovar uma vez após rejeição do access token e repetir a leitura.

Publicar a ConfApi compatível antes do novo app. A implantação inicial pode encerrar sessões Bearer antigas que só existiam na memória; elas não podem ser recuperadas depois que essa memória foi perdida. O limite de dois minutos também não promete recuperar uma rotação confirmada no servidor cuja resposta permaneça perdida além desse prazo. Depois de nova autenticação com DPoP e aparelho aprovado, a persistência protege contra os próximos reinícios. Validar a configuração e a versão do Manager em homologação; testes H2 não comprovam o schema nem a implantação em produção.

Validação: suites `MobileSessionsTest`, `MobileSessionRepositoryTest`, `MobileSessionPersistenceTest`, `MobileSessionAvailabilityTest`, `MobileProofsTest`, `MobilePersistenceApiCompatibilityTest`, `MobileApiTest`, `MobileRoutesTest` e `MobileRenewalRecoveryTest`. Os testes novos incluem a rota HTTP real com MockMvc, recuperação pós-reinício, perda da confirmação do commit, concorrência entre instâncias, replay de prova, outro aparelho, operação diferente, rotação antiga, recibo expirado, logout e revogação. Todos usam mocks/H2, sem chamadas a bancos ou contas reais.
