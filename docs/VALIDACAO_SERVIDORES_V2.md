# Validação dos servidores antes da atualização do app — 05/11/2026

Nesta etapa, os quatro projetos passaram em 152 testes selecionados e geraram seus WARs. O app não foi alterado. O código executável do CService não mudou nesta etapa; sua suíte de 23 testes e seu JAR continuam sendo os da validação anterior. O contrato OpenAPI passou para 2.0.0-draft.3 (16 operações CService, 20 Manager), com referências internas verificadas.

| Projeto | Java | Testes nesta etapa | Pacote |
|---|---|---:|---|
| Manager | 17 | 22 | target/confiancamanger-0.0.1-SNAPSHOT.war |
| Hub | 17 | 11 | target/Sandbox-0.0.1-SNAPSHOT.war |
| confApi | 17 | 28 | target/confApi-0.0.1-SNAPSHOT.war |
| Payara | 17 | 91 | target/ConfiancaTravelPayara-1.0.war |

Todos os testes acima passaram sem falhas, erros ou testes ignorados. Os quatro comandos Maven package terminaram com BUILD SUCCESS. Não foi executada a suíte inteira dos projetos legados nem inicializado nenhum sistema contra banco real.

## Suítes

- Manager: V2ManagerTest, V2IdentityTest, V2SecurityHttpTest, UsuarioAutenticacaoTokenTest.
- Hub: V2IdentityTest, MobileSessionFilterTest, JwtRevocationTest, CServicesRestControllerJaxStartupTest.
- confApi: V2IdentityTest, MobileSessionFilterTest, JwtRevocationTest, PlantaoTokenEncryptedResponseTest, UsuarioApiTest.
- Payara: V2IdentityTest, PortalV2ClientTest, PortalSessionGuardTest, SupportRequestGuardTest, DispositivoAprovacaoBeanTest, FiltroAutenticacaoTest, LoginBeanSecurityTest, CodigoAcessoSecurityTest, CodigoAcessoSafeguardTest, TokenCservicesHttpSecurityTest, SessionListenerSecurityTest.

Comando em cada pasta: mvn -o -Dmaven.repo.local=C:\Users\ti\.m2\repository -Dtest=<suítes acima> package. JAVA_HOME=C:\Program Files\Java\jdk-17; Maven=C:\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd. Logs: D:\Projetos\cServices3.0\.integration-work\<projeto>-package.log. Relatórios: target/surefire-reports.

## Alterações e evidências desta etapa

- JWT individual do Hub/confApi exige consulta ao Manager também com piloto desligado. Rejeição e indisponibilidade não permitem validação apenas pela assinatura.
- Cadeia HTTP real de segurança do Manager exercitada em contexto isolado: leitura de revogação autenticada continua disponível com piloto desligado; novo login fica fechado; identidade OPERATOR não entra como SERVICE.
- Solicitações vencidas não aparecem na fila ativa, mas seu histórico permanece. Contato verificado recebe nova referência quando alterado e não aprova o aparelho.
- Tela Payara registra contato verificado e consulta histórico, além de aprovar/rejeitar. Testes cobrem evidência obrigatória, separação de ações, campos inválidos e descarte de dados antigos após erro.
- Ações de suporte exigem POST, estado JSF de postback, HTTPS e token CSRF da view. Testes recusam código ausente/incorreto, GET e HTTP.
- Sessão de navegador v2 é revalidada: revogação encerra e redireciona antes de executar página; indisponibilidade retorna 503 preservando a sessão. Sessões legadas mantêm seu contrato.
- XML da página de suporte e sintaxe do verificador PowerShell foram validados localmente. A tela ainda precisa de teste no navegador com Payara de homologação.

## Limites

Manager/H2 e doubles locais não comprovam migração e concorrência no MySQL real. Não houve publicação, execução de migração real, envio de e-mail nem alteração do aplicativo mobile. Ainda é necessário configurar TLS, credenciais rotativas, JWKS, operadores iniciais e serviço de mensagens; revisar titularidade/empresa das APIs de negócio; testar os consumidores legados; validar a experiência de suporte e login em homologação.

A migração do Manager e as credenciais para consulta de revogação são pré-requisitos da implantação, mesmo antes da ativação do piloto. A lista de rotas mobile permanece fechada por padrão. Não migrar contas reais para v2 antes de disponibilizar o app compatível.

## Alinhamento posterior ao schema V3

O conjunto foi revalidado com oito tabelas no cService3, mantendo AUTH_MOBILE_ATIVO e os contratos HTTP v2. Resultados atuais: [ALINHAMENTO_APLICACOES_V3.md](ALINHAMENTO_APLICACOES_V3.md). Pacotes e hashes: PACOTES_SERVIDORES_V2.json. Este documento preserva os resultados das etapas anteriores. Nenhuma migracao foi executada em banco real nesta atualizacao.
