> Resultado da etapa anterior de quatro dígitos. Para a revisão posterior e os pacotes atuais, consulte [REVISAO_INTEGRACAO_20261106.md](REVISAO_INTEGRACAO_20261106.md).

# Validação da correção dos tokens Confiança

Data: 06/11/2026. Esta revisão substitui os resultados de 05/11/2026.

O cServices2.0 gera tokens com SecureRandom.nextInt(9000) + 1000. Esse mesmo intervalo de quatro dígitos foi restaurado no cService3, tanto na geração sem persistência quanto na emissão persistida. Manager, confApi, Hub, Payara e App foram alinhados para os códigos Confiança de quatro dígitos. Matriz física continua com quatro; o Safeguard mantém seu comportamento anterior de seis dígitos. Os demais propósitos, regras e rotas não foram alterados nesta correção.

| Projeto | Testes aprovados |
| --- | ---: |
| cservice | 27 |
| manager | 58 |
| confapi | 29 |
| hub | 53 |
| payara | 130 |
| App (40 suítes Jest) | 359 |
| Total | 656 |

TypeScript sem erros. Os cinco JAR/WAR foram recompilados; caminhos e hashes estão em PACOTES_SERVIDORES_V2.json. Não foi gerado APK/IPA.

Regressões verificadas: geração no intervalo antigo; aceitação de quatro dígitos e rejeição de seis nos contratos Confiança; preservação de zeros à esquerda nas entradas; confirmação do aparelho, login, plantão e antifraude; limite dos campos do Payara; testes existentes de Safeguard. Mantidos os testes de isolamento por contexto, tentativas, expiração e consumo único.

As suítes dos servidores foram selecionadas para os fluxos afetados e usam H2/mocks. Não foram executados testes ApplicationTests que inicializam os serviços completos com suas configurações reais. O App executou todas as suas suítes Jest. A primeira execução do novo teste HTTP do cService usou um ID fictício fora do formato da rota; o teste foi corrigido para usar UUID e a execução final passou.

Nenhum banco real foi acessado ou alterado. Nenhum serviço foi reiniciado, publicado ou desligado. O cServices2.0 não foi modificado. Integração real por HTTPS, envio real de e-mail e execução em aparelho não foram realizados nesta etapa.

Logs e cópias anteriores dos arquivos alterados: D:\Projetos\cServices3.0\.local\token-four-digits.
