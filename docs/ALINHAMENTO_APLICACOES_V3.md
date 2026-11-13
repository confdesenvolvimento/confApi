> HISTÓRICO: substituído pela [arquitetura atual](ARQUITETURA_TOKENS_E_AUTENTICACAO.md). Não executar as instruções de migração do antigo cService. O cService atual não usa banco.

# Alinhamento das aplicações ao schema V3 — 05/11/2026

Registro histórico da consolidação de tabelas. A adequação posterior ao plano original e ao cadastro dispositivo está em [ADEQUACAO_PLANO_AUTENTICADOR.md](ADEQUACAO_PLANO_AUTENTICADOR.md); ela atualiza o fluxo e o contrato para draft.5. As contagens e descrições abaixo pertencem à etapa anterior.

O modelo definitivo desta etapa mantém oito tabelas no cService3, incluindo AUTH_MOBILE_ATIVO. Schema V3 identifica a migração de armazenamento; o contrato HTTP permanece 2.0.0-draft.4, com rotas /api/v2 e /internal/v2.

## Responsabilidade por aplicação

| Aplicação | Integração mantida | Acesso às oito tabelas |
|---|---|---|
| cService3 — D:\Projetos\cServices3.0 | Chaves, cadastro, ativação, sessões, refresh, desafios, controles e auditoria | Direto, no CSERVICES |
| Manager — D:\confiancaManager1 | Aprovação e versão vigente do aparelho; resultado do desafio por API | Nenhum; mantém sete AUTH_V2_* próprias no ConfiancaManger |
| Hub — D:\hubconfianca | Relay do desafio e introspecção de sessão por API | Nenhum |
| confApi — D:\ConfApi | Autenticação primária/conclusão pelo Manager e introspecção pelo cService3 | Nenhum |
| Payara — D:\Projetos\ConfiancaTravelPayara | Tentativa do navegador, validação de código e conclusão pelo Manager | Nenhum |
| App — D:\ConfiancaApp | Sessões DPoP, cadastro, confirmação, ativação, perfil e código do portal | Nenhum |

A revisão dos clientes não identificou dependência das quatro tabelas removidas. Endpoints, campos JSON, escopos e cabeçalhos permanecem compatíveis; não foi necessário modificar o código executável dos consumidores para esta redução. Os guias foram alinhados, o comando package foi executado nos cinco backends e seus artefatos foram validados a partir dos projetos atuais. Não foram sobrescritas alterações de código já presentes nos projetos.

AUTH_MOBILE_CODIGO foi incorporada aos campos code_* de CADASTRO/DESAFIO; AUTH_MOBILE_OUTBOX aos campos delivery_* de CADASTRO; REPLAY e LIMITE à tabela CONTROLE, com tipos separados. ATIVO conserva a exclusão mútua por usuário durante a ativação. Sessões, rotação de refresh, revogação e auditoria continuam preservadas.

## Banco e implantação

Nenhum banco real foi acessado ou alterado nesta atualização. Nenhum serviço foi iniciado ou publicado. Os testes de banco usam H2 em memória e os demais usam serviços simulados.

O cService3 mantém Flyway desabilitado por padrão e no perfil local. O JAR exige a estrutura V3; a estrutura V1 ou V2 será recusada com SCHEMA_AUTH_V3_PENDENTE. Não habilitar migrações para contornar esse diagnóstico no banco de produção. As condições de migração, incluindo bloqueio da V2 quando há códigos/entregas e preservação de controles na V3, estão em REDUCAO_TABELAS.md.

As sete tabelas AUTH_V2_* do Manager continuam sendo um pré-requisito separado. A ausência dessas tabelas, identificada no diagnóstico anterior do login, não foi resolvida pela redução no cService3. Esta atualização não cria tabelas, não libera o login por outro caminho e não habilita o piloto. Uma implantação futura requer os dois schemas compatíveis em ambiente autorizado.

Os guias e os manifestos apontam para D:\Projetos\cServices3.0, a pasta ativa. A outra cópia em D:\Projetos\cService3.0 não foi atualizada nesta etapa e não deve ser usada como fonte do pacote atual.

## Validação e pacotes

Todos os comandos Maven package concluíram com BUILD SUCCESS, sem falhas, erros ou testes ignorados. O app passou na verificação TypeScript e em sua suíte JavaScript completa.

| Projeto | Testes aprovados | Artefato ou verificação |
|---|---:|---|
| cService3 | 48 | target/cService3.jar |
| Manager | 24 | target/confiancamanger-0.0.1-SNAPSHOT.war |
| Hub | 12 | target/Sandbox-0.0.1-SNAPSHOT.war |
| confApi | 29 | target/confApi-0.0.1-SNAPSHOT.war |
| Payara | 91 | target/ConfiancaTravelPayara-1.0.war |
| App | 356 em 40 suítes | TypeScript e Jest; sem build nativo nesta etapa |

Total: 204 testes de backend e 356 do app. Os caminhos absolutos, tamanhos, hashes SHA-256 e classes verificadas nos pacotes estão em PACOTES_SERVIDORES_V2.json. O manifesto identifica esta etapa em alignmentStage, separadamente das validações históricas. O JAR contém V2/V3 e SchemaCompatibility, não inclui a configuração privada e conserva Flyway e tarefas de entrega desabilitados no perfil local.

Logs locais: D:\Projetos\cServices3.0\.local\alignment-eight-tables. Comando dos backends: Maven 3.9.9 offline, cache C:\Users\ti\.m2\repository, objetivo package, Java 17 nos quatro projetos antigos e Java 21 no cService3. O app foi validado com Node 24, tsc --noEmit e jest --runInBand --watchAll=false.

Suítes selecionadas:

- Manager: V2ManagerTest, V2IdentityTest, V2SecurityHttpTest, MobileProfileTest, UsuarioAutenticacaoTokenTest.
- Hub: V2IdentityTest, MobileSessionFilterTest, MobileRegistrationTest, JwtRevocationTest, CServicesRestControllerJaxStartupTest.
- confApi: V2IdentityTest, MobileSessionFilterTest, MobileRegistrationTest, JwtRevocationTest, PlantaoTokenEncryptedResponseTest, UsuarioApiTest.
- Payara: V2IdentityTest, PortalV2ClientTest, PortalSessionGuardTest, SupportRequestGuardTest, DispositivoAprovacaoBeanTest, FiltroAutenticacaoTest, LoginBeanSecurityTest, CodigoAcessoSecurityTest, CodigoAcessoSafeguardTest, TokenCservicesHttpSecurityTest, SessionListenerSecurityTest.
- cService3: suíte completa (AuthenticationTest, SchemaReductionMigrationTest, SecurityControlMigrationTest, SharedDatabaseMigrationTest).

As suítes selecionadas dos servidores cobrem identidade, prova, revogação, autorização, concorrência, perfil, relay e sessões de navegador. Elas não equivalem à execução completa dos sistemas legados nem a um teste integrado em MySQL/Payara reais.

No app, a validação de TypeScript e testes JavaScript não substitui o build e a execução nativos. Nenhum APK/AAB foi gerado nesta etapa; iOS requer macOS/Xcode. Permanecem pendentes a homologação com TLS, tokens técnicos rotativos, mensagens deduplicadas, operadores e aparelho físico, além da revisão de permissões por rota de negócio.
