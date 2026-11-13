# Validação inicial da integração v2 — 05/11/2026

Atualização posterior: [VALIDACAO_SERVIDORES_V2.md](VALIDACAO_SERVIDORES_V2.md) registra 152 testes dos quatro servidores e os WARs gerados. Os resultados abaixo são da etapa inicial.

120 testes selecionados passaram, sem falhas, erros ou testes ignorados. Nenhum sistema foi iniciado contra um banco real. Não se trata de certificação de produção nem da execução de todas as suítes dos projetos antigos.

| Projeto | Java | Testes | Suítes executadas |
|---|---|---:|---|
| Manager | 17 | 18 | V2ManagerTest, V2IdentityTest, UsuarioAutenticacaoTokenTest |
| Hub | 17 | 8 | V2IdentityTest, MobileSessionFilterTest, CServicesRestControllerJaxStartupTest |
| confApi | 17 | 25 | V2IdentityTest, MobileSessionFilterTest, PlantaoTokenEncryptedResponseTest, UsuarioApiTest |
| Payara | 17 | 46 | V2IdentityTest, PortalV2ClientTest, LoginBeanSecurityTest, CodigoAcessoSecurityTest, CodigoAcessoSafeguardTest, TokenCservicesHttpSecurityTest |
| CService | 21 | 23 | AuthenticationTest |

Comando em cada pasta: mvn -o -Dmaven.repo.local=C:\Users\ti\.m2\repository -Dtest=<suítes da linha> test. Maven usado: C:\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd. JAVA_HOME apontou para C:\Program Files\Java\jdk-17 ou jdk-21. Relatórios Surefire: target/surefire-reports em cada projeto. Logs da execução: D:\Projetos\cServices3.0\.integration-work\<projeto>-test.log.

Cobertura relevante:

- JWT: assinatura, audiência exata, tipo, escopo, expiração e duração máxima; token técnico antigo recusado no contrato v2.
- Manager: pendência não sobrescreve ativo, aprovação exige MFA recente e não permite autoaprovação; contato/OTP não aprovam; idempotência não permite trocar candidato; duas aprovações concorrem por uma versão; histórico permanece e dispositivo antigo não pode ser reativado.
- Migração: conta migrada não volta ao login/mutação antigos ao desligar o piloto. Conclusão consome referência uma vez e não transforma MFA de aparelho anterior em token da versão nova. Token individual é recusado após a substituição.
- CService: prova de chave, replay, sessão provisória sem negócio, código ligado a usuário/cliente/sessão/finalidade, consumo concorrente único, expiração, zeros iniciais, limites persistidos, refresh reutilizado e outbox cifrada com retry.
- Gateways: acesso DPoP fechado até revisão de rota/método; identidade vem da introspecção; indisponibilidade bloqueia. CService valida origem por serviço e versão atual.
- Payara: primário sem token de negócio, confirmação ligada à mesma sessão, identidade divergente recusada, falha não escolhe legado, cancelamento limpa desafio; provedor definido no início impede alternância por quantidade de dígitos.
- Regressões selecionadas: emissão individual do Manager, inicialização do adaptador Hub, plantão/confApi e controles de sessão/MFA/HTTP do Payara.

Ambiente: Manager usa contexto mínimo e H2 descartável com migração aditiva; CService usa Spring/MockMvc, H2/Flyway e Manager simulado. Gateways/Payara usam doubles locais, sem rede de produção. Mocks não comprovam TLS, regras de rede, entrega real de e-mail, banco MySQL, aplicação JSF completa ou chave nativa no aparelho.

A validação no MySQL de homologação, a execução integrada dos cinco serviços e do app, os fluxos de suporte no navegador, a deduplicação no serviço de e-mail, as permissões por recurso/empresa e a regressão completa dos consumidores legados permanecem necessárias antes da publicação.
