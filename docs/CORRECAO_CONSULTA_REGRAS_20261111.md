# Consulta de regras na seleção de trechos — 11/11/2026

A ConfApi passou a consultar as regras pelo `ChatConfiancaManagerClient`, que compartilha o token técnico, renova a autenticação uma vez após HTTP 401 e utiliza os limites de tempo do cliente do chat. Antes, a consulta de regras tinha autenticação separada, não recuperava HTTP 401 e podia seguir sem autenticação quando a obtenção do token falhava.

Respostas vazias ou inválidas continuam sendo falhas de consulta; recusas comerciais e a exigência de remarcação conjunta continuam sendo respeitadas.

## Validação

- 138 testes selecionados da ConfApi passaram: cliente de regras (8), fluxo de remarcação (109), controller (9), cliente do Manager (5) e provedor de token (7).
- Os testes de fluxo cobrem seleção individual e ida e volta com HTTP 401 seguido de renovação bem-sucedida. Ambos avançam para `AGUARDANDO_CRITERIOS`.
- 7 testes de regras do Manager passaram: consulta (3) e múltiplos trechos (4).
- Consultas autorizadas ao Manager publicado, com dados sintéticos G3/LIGHT para um e dois trechos, retornaram HTTP 200 e `PERMITIDA_COM_MULTA`, regra 44. O cliente corrigido também recebeu essa regra.
- O WAR foi gerado com `mvn -o -DskipTests package`, após os testes, e a classe corrigida dentro do arquivo foi conferida por SHA-256 contra a classe compilada.

## Limites do diagnóstico

Não foi possível reproduzir a falha histórica do vídeo no Manager publicado: as consultas realizadas responderam corretamente. A ausência de recuperação após HTTP 401 foi reproduzida no cliente anterior e corrigida, mas não há evidência de que HTTP 401 tenha sido a causa exata do vídeo.

Não foi necessária alteração de código ou cadastro de regras no Manager. Nenhuma reserva foi alterada. Os testes do fluxo usam respostas controladas e não substituem a validação no aplicativo após publicação.

## Pacote

`target/correcao-regras-20261111/confApi4.war`, acompanhado de `SHA256SUMS.txt`.

O pacote foi gerado a partir da árvore de trabalho atual da ConfApi, incluindo alterações que já existiam antes desta correção. O arquivo anterior `target/confApi4.war` foi preservado. Este trabalho não publicou o novo pacote no servidor; sua publicação e a validação no aplicativo permanecem pendentes.
