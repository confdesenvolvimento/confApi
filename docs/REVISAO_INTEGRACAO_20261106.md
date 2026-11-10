# Revisão das integrações de autenticação — 06/11/2026

## Atualização: revisão ampla das sete aplicações

**O conjunto ainda não está pronto para entrada em operação completa.** Foram compilados os sete projetos e executadas as suítes locais. Os testes que inicializam a aplicação real com datasource externo e o teste opcional de MySQL foram excluídos explicitamente. Não foram executados scripts no banco, login real, ativação de aparelho ou envio de mensagens.

| Aplicação | Testes executados | Falhas de asserção | Erros de execução | Resultado |
| --- | ---: | ---: | ---: | --- |
| cService3 | 33 | 0 | 0 | Compilação e testes aprovados |
| Manager | 1.794 | 11 | 58 | Compila; geração de pacote bloqueada pelos testes |
| confApi | 1.508 | 19 | 7 | Compila; geração de pacote bloqueada pelos testes |
| Hub | 1.070 | 9 | 0 | Compila; geração de pacote bloqueada pelos testes |
| ConfiançaAdmin | 51 | 0 | 0 | Compilação e testes aprovados |
| Payara | 3.428 | 0 | 0 | Clean install aprovado e WAR republicado localmente |
| ConfiançaApp | 361 em 41 suítes | 0 | 0 | Jest e TypeScript aprovados; compilação nativa não validada |

São 8.245 casos na última execução de cada suíte, com 104 casos ainda reprovados. Reexecuções não foram somadas a esse total. Os testes selecionados de autenticação/tokens passaram dentro das suítes: Manager 96, confApi 32 e Hub 55. Isso não substitui um teste completo por HTTPS com dois aparelhos.

Correções concluídas nesta etapa:

- **Payara:** quatro entradas de teste ainda tinham seis dígitos. Foram restauradas para quatro; a rotina de produção não foi alterada. O NetBeans havia removido o portal antes de o build falhar; o WAR foi republicado. Login completo confirmado em `http://localhost:8085/ConfiancaTravelPayara/`.
- **confApi:** havia somente a API de Bean Validation, sem uma implementação. Incluído `spring-boot-starter-validation`, compatível com o Spring Boot existente. Os 22 testes do controlador rodoviário passaram, incluindo os sete casos inválidos que antes recebiam HTTP 200 indevidamente. Esta alteração de dependência está no projeto; ainda não foi implantada no processo em execução. Requer recarga Maven e nova inicialização após as correções pendentes.
- **Hub:** contextos isolados não registravam o filtro mobile e dois serviços simulados necessários. Foram corrigidos quatro arquivos de teste, preservando as verificações de autorização e as regras reais. Os 21 erros de inicialização foram eliminados; permanecem nove falhas funcionais de teste em fornecedores.
- **App:** `npm start`, `android`, `ios` e `web` usam o iniciador existente que seleciona Node compatível. `npm start -- --help` confirmou Node 24.22.1, mesmo com Node 18.17.1 no PATH do Windows. Nenhuma instalação global foi alterada.
- **Configuração:** os quatro verificadores PowerShell antigos exigiam variáveis de autenticação no cService e uma lista de operadores obsoletas. Foram alinhados ao Manager, incluída a opção Admin e verificadas oito situações sintéticas. Esses scripts só inspecionam o ambiente do processo que os executa; não habilitam funcionalidades nem validam credenciais em servidores.

Pendências que impedem aprovar o conjunto:

1. **Manager — 69 casos:** arquivos SQL de referência ausentes em testes de Central de Viagens, contatos, cadastros de chat e idempotência; dependências simuladas não inicializadas em reservas/pacotes; schema H2 de histórico sem uma coluna esperada. As falhas não autorizam criar tabelas em produção e exigem correção dos recursos de teste e conferência dos contratos envolvidos.
2. **confApi — 26 casos:** restrições de agência/unidade no chat (três casos), contratos/configuração do chat (22 casos) e sincronização da pesquisa progressiva de hotel (um caso). A divergência de autorização merece prioridade; não foi removida nenhuma asserção para mascará-la.
3. **Hub — nove casos:** emissão aérea de pacote e roteamento/contratos de HotelDO, EZLink e disponibilidade. Precisam ser conciliados com os contratos atuais de cada fornecedor.
4. **Configuração V2:** as variáveis necessárias não estão presentes nos ambientes Process/User/Machine inspecionados. Configurações próprias da JVM/IDE também precisam ser conferidas. Faltam HTTPS, identidades técnicas, chaves, renovação de tokens e entrega. A coluna de aprovador já foi criada manualmente pelo usuário, mas a habilitação inicial do aparelho do primeiro aprovador continua pendente.
5. **App e serviços de negócio:** `API_CONFIG` ainda contém HTTP/localhost; `carro.service.ts` e `notificacao.service.ts` ainda acessam o Manager diretamente. A migração App → confApi → Manager, autorização por usuário/empresa, filtros, paginação e uploads não está concluída. Não há `.env` local com a URL HTTPS do autenticador. A autenticação nativa não funciona no Expo Go.
6. **Compilação nativa:** não encontrados Android platform 36, Build Tools 36.0.0 e NDK 27.1.12297006 na instalação de SDK verificada. Esses valores coincidem com o catálogo de versões do React Native instalado. Nenhum APK/IPA foi produzido nesta revisão.
7. **Dados e integração real:** permanecem a conferência manual de duplicidades, vínculos órfãos e índices do Manager, além do teste completo de solicitação, aprovação por outro usuário, ativação do novo dispositivo e recusa do antigo.

O Admin também respondeu com login completo em `http://localhost:8083/ConfTravelAdmin_war/`. Servidores de produção não foram atualizados. Os tokens Confiança continuam com quatro dígitos; nenhuma nova tabela foi proposta nesta etapa.

Evidências atuais: `docs/REVISAO_APLICACOES_20261106.json`; detalhes dos casos reprovados em `.local/review-all-20261106/failed-tests.json`; logs por projeto na mesma pasta. As seções seguintes registram a revisão anterior de autenticação e seus testes específicos, não o resultado da suíte ampla acima.

## Registro anterior de autenticação

Foram revisados cService3, Manager, confApi, Hub, Payara e ConfiançaApp, com comparação do serviço antigo e do plano inicial. O escopo foi geração/validação de tokens, cadastro e troca de aparelho, sessões, aprovação, plantão, antifraude, matriz e os pontos de integração com as APIs de negócio. Não é uma auditoria integral de todos os módulos comerciais dos seis repositórios.

## Regras preservadas

- Tokens Confiança com quatro dígitos; geração dinâmica de 1000 a 9999, igual ao cServices2.0. A proposta de seis dígitos do documento não foi tratada como decisão aprovada.
- Safeguard permanece separado, com seu contrato anterior. O gerador antigo de senha de atendimento do Hub também já usava seis dígitos e não foi confundido com o token Confiança.
- Autenticador do App entra pelo confApi; Manager mantém dispositivo, aprovação, solicitações e histórico no banco do Manager. O cService3 continua independente e não recuperou tabelas de autenticação do App.
- Nenhuma alteração de schema, acesso ao banco real, envio de mensagens, deploy ou reinicialização de serviços. O serviço antigo não foi alterado nem desligado.

## Correções aplicadas nesta revisão

| Ponto | Problema encontrado e correção | Verificação |
| --- | --- | --- |
| Payara/plantão | Uma tentativa incorreta descartava o contexto e a tela encerrava o desafio. O mesmo desafio agora permite nova tentativa, respeitando o limite e o vencimento mantidos pelo Manager. Sucesso ou expiração encerra o contexto. | PlantaoTokenConfApiTest; limites já cobertos no Manager |
| Payara/prazo | O contador da tela fixava 300 segundos e não acompanhava expiresAt. A tela agora usa o vencimento retornado pelo servidor; nova tentativa não reinicia o prazo. | Três testes JavaScript, incluindo atualização AJAX e remoção do componente |
| confApi e Hub/sessão | Recusa da identidade durante introspecção podia chegar como 403, mantendo o App na tela autenticada. Recusa da sessão agora chega como 401; uma negativa normal de permissão do recurso continua 403; indisponibilidade continua 503. | MobileSessionFilterTest e testes do cliente do App |
| Manager/prova do aparelho | Falha de persistência da proteção contra repetição era confundida com credencial inválida. Agora retorna indisponibilidade, sem autorizar a operação nem imputar falha à credencial. | MobileAuthenticationTest |
| Identidade técnica | Validadores compartilhados aceitavam configurações de emissor vazio, subject vazio ou expiração igual à emissão. Esses casos agora são recusados nos quatro consumidores. | V2IdentityTest em Manager, confApi, Hub e Payara |
| cService3/matriz | Duas matrizes da mesma empresa com a mesma chave podiam ter suas posições misturadas. A emissão recusa ambiguidade e o catálogo impede cadastrar a duplicação. Trocar a chave invalida desafios anteriores; restaurar a chave não os reativa. | TokenLifecycleTest |
| Manager/logs | Um método legado de plantão imprimia login e código na URL. A impressão foi removida. O contrato antigo em URL continua uma pendência de migração, descrita abaixo. | Inspeção do código |

Principais arquivos: TokenLifecycle.java e TokenCatalog.java no cService3; Proofs.java e PlantaoService.java no Manager; MobileSessionFilter.java e V2.java nos gateways; PlantaoTokenConfApi.java, PlantaoBean.java e plantaoAtendimento.xhtml no Payara. As cópias anteriores e o manifesto exato dos arquivos alterados estão em .local/review-20261106.

## Pendências antes da descontinuação do serviço antigo

1. **Completar as APIs de negócio do App.** A autenticação v2 não significa que todos os serviços comerciais já foram migrados. Em D:/ConfiancaApp/shared/service/carro.service.ts e notificacao.service.ts há chamadas usando API_CONFIG.baseUrl diretamente para rotas do Manager. API_CONFIG, em config/api.config.tsx, ainda aponta os serviços de desenvolvimento para HTTP/localhost. O transporte nativo exige HTTPS e localhost no celular não aponta para o computador de desenvolvimento. Essas chamadas precisam seguir App → confApi → Manager, com autorização de usuário/empresa na entrada.
2. **Preservar filtros, paginação e arquivos nessa migração.** shared/authv2/resource-policy.ts recusa qualquer query e shared/authv2/http.ts recusa FormData/formatos binários. Há usos atuais em notificacao.service.ts (page/size), hotel.service.ts e carro.service.ts (buscas), e chatia.service.ts (áudio/arquivos). Apenas preencher EXPO_PUBLIC_AUTH_V2_RESOURCE_ROUTES não resolve esses casos. Cada rota e parâmetro precisam ser adequados e testados; não se deve liberar todas as rotas genericamente. Nos servidores, a cadeia V2Security também reserva /api/v2/** para identidade técnica: uma API de negócio DPoP nesse prefixo precisa de separação explícita das rotas internas e de suporte.
3. **Migrar as automações legadas de plantão.** Manager/wooba/Chat/plantaoToken/PlantaoService.java ainda usa Hub /api/token/generate e /api/token/validate com parâmetros em URL. TokenControllerManger/TokenManager mantém esse contrato antigo, independente do cService3. A validação antiga de cService por login+código foi encerrada. É necessário migrar os consumidores conhecidos para o contexto da operação e inventariar outros consumidores externos antes de retirar instâncias. Nenhum fluxo externo foi desligado por esta revisão.
4. **Preparar homologação isolada.** Conferir os schemas e scripts preparados em uma cópia de testes, sem operar o banco de produção. Configurar HTTPS, URLs, identidades, chaves e renovação das credenciais técnicas curtas. As tabelas reais não foram inspecionadas e os scripts não foram executados no banco real. Não criar tabelas de autenticação mobile no CSERVICES.
5. **Validar entrega e suporte.** Conferir o contrato real do serviço de e-mail, a notificação de pendência, deduplicação, aprovação assistida e recuperação de aparelho. Nesta revisão nenhuma mensagem foi enviada. A integração externa de e-mail não faz parte dos seis projetos revisados.
6. **Compilar e testar o App nativo e o conjunto por HTTPS.** O código nativo foi inspecionado, mas não houve compilação Android/iOS nem teste em aparelho. O módulo depende de Android 11+ com chave em hardware ou Secure Enclave no iOS; isso precisa de prova em aparelhos suportados. Expo Go não carrega esse módulo. A última verificação registrada de SDK indicou componentes Android incompletos. Executar os fluxos completos de cadastro, aprovação, troca, login, código incorreto, consumo, expiração, revogação, plantão, antifraude e matriz física antes do corte.

As pendências de rotas de negócio e automações não foram resolvidas criando um encaminhamento genérico ou reabrindo validação global por número. Exigem adaptação dos contratos e verificação da autorização de cada operação. A migração completa ainda não está pronta para descontinuar o serviço antigo.

## Testes desta revisão

| Projeto | Testes aprovados |
| --- | ---: |
| cservice | 30 |
| manager | 62 |
| confapi | 34 |
| hub | 58 |
| payara | 137 |
| App — três suítes de autenticação | 45 |
| JavaScript do plantão Payara | 3 |
| Total executado nesta revisão | 369 |

Os cinco servidores foram recompilados e seus pacotes gerados com sucesso. Hashes e caminhos atuais estão em PACOTES_SERVIDORES_V2.json. Maven foi executado offline com testes selecionados e H2/mocks; os testes que carregam configurações reais dos serviços não foram executados. Nenhum teste integrado contra produção foi feito.

O App não teve código alterado nesta revisão. Foram repetidas somente as suítes relacionadas à autenticação; os 359 testes do App e a checagem de TypeScript passaram na etapa anterior de quatro dígitos, conforme .local/token-four-digits. Esses 359 não foram somados novamente ao total desta revisão.

Logs atuais: D:/Projetos/cServices3.0/.local/review-20261106.
