# Testes do ConfApi rodoviário no Postman

Importe a collection e o environment JSON desta pasta. Selecione o environment antes de enviar requisições. O guia PDF está em `../../output/pdf/guia-testes-confapi-rodoviario-postman.pdf` no repositório e na raiz do pacote ZIP.

1. Ajuste baseUrl (padrão http://localhost:8088, sem barra final) e o contexto do WAR, quando houver.
2. Preencha appToken com um JWT aceito pelo ConfApi, sem Bearer. /user/auth também está protegido na configuração atual: a primeira chamada da collection apenas renova um acesso já autenticado. Para o primeiro token, use o fluxo já aprovado no ambiente. /api/auth/token é rotina de plantão, não login JWT de aplicação.
3. Preencha loginUsuario e senhaUsuario e execute 02 Login individual Manager. O script guarda chaveToken em usuarioToken; não use tokenAcesso como JWT individual.
4. Configure codgSistema, codgAgencia, códigos externos e vínculos do Manager. Na reserva, os campos dadosManager.trechos[].trecho.codgCidadeOrigem/codgCidadeDestino recebem codgVinculoCidadeSistema. Na pesquisa, origem/destino recebem codgCidade da tabela cidade.
5. Para pesquisar, preencha codgCidadeOrigem e codgCidadeDestino com IDs da tabela cidade. O Hub resolve os codigos externos pelo Manager antes de chamar o fornecedor. Defina data futura, pesquise e selecione manualmente codigoViagem e assento. O exemplo de compra é direto, com um passageiro. Para conexões/ida e volta, informe todos os trechos efetivos e poltronas de conexão.
6. Preencha passageiro, sexo conforme cadastro, email e chaveCompra única de 8 a 64 caracteres.
7. A requisição 14 Reservar emitir e gravar Manager dispensa as variáveis de habilitação. Para as demais operações com efeito, defina permitirOperacoesComEfeito=true e acaoPermitida com a ação exata. Execute individualmente. Nessas demais operações, a ação é limpa depois da resposta.

Ações aceitas em acaoPermitida: emitir, cancelarReserva, cancelarBilhete, adicionarNota, registrarRecebimento. reservar faz booking e checkout, podendo cobrar, e não exige acaoPermitida. emitir é uma alternativa para booking existente; não a execute para repetir uma compra já reservada. preApproved depende da habilitação da conta. PIX/cartão precisam seguir o contrato do Hub e não são tokenizados pela collection.

O bloqueio usa pm.execution.skipRequest() em pre-request. Documentação oficial: https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution/ . Use uma versão com suporte à API. Não execute todas as pastas no Runner como se fossem um único fluxo automático.

## Resultados e recuperação

HTTP 200 pode conter success=false ou requerConferencia=true. Os testes positivos marcam essas respostas para revisão; não repita checkout para resolver timeout.

Antes de reservar/emitir, o script salva payloadRecuperacao com os metadados e chave originais, sem campos de cartão/comprador e sem recebimento manual adicional. A requisição 17 usa essa snapshot e nunca cria novo booking ou checkout. Se outra tentativa substituir a snapshot, restaure os dados da compra anterior antes de recuperá-la. Referência ausente retorna 409 e exige conferência no fornecedor. A verificação por referência não substitui idempotência de checkout e não garante exclusão entre várias instâncias.

Confirmada 1 exige compra paid e todos os bilhetes emitted com número. Compra pendente fica 0. Cancelada 2 exige compra e todos os bilhetes canceled. Cancelamento parcial exige conferência operacional, pois o Manager atual impede editar componentes de reservas confirmadas. Não há estorno automático.

Registrar recebimento apenas concilia uma transação existente; exige usuário interno e chave própria, sem cobrar cartão ou confirmar PIX. Conserve a chave ao repetir o mesmo registro.

## Variáveis

| Variável | Inicial | Uso |
| --- | --- | --- |
| `baseUrl` | http://localhost:8088 | URL sem barra final; inclua o contexto do WAR. |
| `prefix` | /v1/rodoviario | Prefixo rodoviario. |
| `appToken` | vazio | JWT aceito pelo ConfApi, sem Bearer. |
| `usuarioToken` | vazio | JWT individual retornado em chaveToken. |
| `loginApp` | vazio | Login para renovacao opcional. |
| `senhaApp` | vazio | Senha local para renovacao opcional. |
| `loginUsuario` | vazio | Login individual do Manager. |
| `senhaUsuario` | vazio | Senha individual local. |
| `sistema` | QueroPassagem | Nome do fornecedor no Hub. |
| `codgSistema` | vazio | ID do sistema no Manager. |
| `codgAgencia` | vazio | ID da agencia autorizada. |
| `codgCidadeOrigem` | vazio | ID da tabela cidade enviado na pesquisa como origem numerica. |
| `codgCidadeDestino` | vazio | ID da tabela cidade enviado na pesquisa como destino numerico. |
| `origemCodigo` | vazio | Codigo externo CIT_n ou ROD_n. |
| `destinoCodigo` | vazio | Codigo externo CIT_n ou ROD_n. |
| `codgVinculoOrigem` | vazio | codgVinculoCidadeSistema da origem. |
| `codgVinculoDestino` | vazio | codgVinculoCidadeSistema do destino. |
| `buscaDestino` | São Paulo | Filtro por nome; limpe para listar todos e pagine. |
| `dataViagem` | vazio | Data futura YYYY-MM-DD. |
| `codigoViagem` | vazio | codigoFornecedor selecionado na pesquisa. |
| `assento` | vazio | Poltrona disponivel escolhida manualmente. |
| `codigoViacao` | vazio | Codigo externo da viacao. |
| `nomePassageiro` | Passageiro | Nome de homologacao. |
| `sobrenomePassageiro` | Teste | Sobrenome. |
| `cpfPassageiro` | vazio | CPF de homologacao com 11 digitos. |
| `dataNascimento` | 2000-01-01 | Nascimento YYYY-MM-DD. |
| `sexoPassageiro` | vazio | Codigo conforme cadastro do Manager. |
| `emailCompra` | vazio | Email da compra. |
| `chaveCompra` | vazio | Chave unica de 8 a 64 caracteres; preservar na recuperacao. |
| `codgReservaRodoviario` | vazio | ID local retornado em reserva.reserva. |
| `codigoCompra` | vazio | Codigo externo da compra. |
| `codigoReservaTemporaria` | vazio | Identificador do booking. |
| `codigoPassageiro` | vazio | ID externo do passageiro. |
| `codgRodoviarioTrecho` | vazio | ID local do trecho. |
| `payloadRecuperacao` | vazio | Snapshot de recuperacao sem campos de cartao. |
| `permitirOperacoesComEfeito` | false | Habilitar chamadas com efeito, exceto a requisicao 14. |
| `acaoPermitida` | vazio | Uma acao por vez; limpa apos a resposta das operacoes que exigem habilitacao. Dispensada na requisicao 14. |
| `motivoCancelamento` | Solicitacao de teste em homologacao | Motivo do cancelamento. |
| `notaHistorico` | Teste rodoviario no Postman | Observacao do historico. |
| `chaveRecebimento` | vazio | Chave propria de 8 a 50 caracteres. |
| `codgFormaPagto` | vazio | Forma de pagamento do Manager. |
| `valorRecebimento` | vazio | Valor ja conciliado com ponto decimal. |
| `dataRecebimento` | vazio | Data YYYY-MM-DD nao futura. |
| `codgTransacao` | vazio | Identificador da transacao ja conciliada. |

## Validação offline da collection

Execute `node docs/postman/validate-postman.cjs` a partir da raiz do repositório. O script usa somente módulos nativos do Node e fixtures, sem chamadas reais.

## Testes negativos e diagnóstico

- Sem Bearer: 401 ou 403, conforme o filtro de segurança implantado.
- Sem X-Usuario-Token, mantendo appToken válido: 401.
- Recurso desconhecido/JSON malformado: 400.
- Código de outra compra: 200 com success=false e requerConferencia=true no service atual.
- Fora de agência/unidade: o Manager recusa o acesso com 403. Na criacao, esse acesso ocorre apos o Hub; o resultado parcial preserva a compra e exige conferencia.
- Manager 401 ou `TOKEN_INDIVIDUAL_INVALIDO`: o header `X-Usuario-Token` precisa conter `chaveToken` do login individual, sem `Bearer`. A mensagem de token obrigatorio do Manager tambem cobre JWT invalido, expirado ou de outro ambiente. Execute **02 Login individual Manager** no mesmo environment. Se ja houver compra no Hub, preserve `chaveCompra` e `payloadRecuperacao` e execute **17 Recuperar sem novo checkout**, sem repetir a requisicao 14. Compras `in_confirmation_process` sao salvas como pendentes (status 0), sem afirmar que os bilhetes foram emitidos.
- Sem resposta nem status HTTP: abra o Console do Postman. A versão anterior pulava a chamada quando appToken estava vazio no environment. Reimporte a collection atualizada; mantenha o environment já preenchido. Agora consultas sem token são enviadas para receber HTTP 401/403. A validação usa os valores efetivos do Postman (pm.variables.get), respeitando a prioridade dos escopos; um valor vazio no environment ainda sobrepõe um token global.
- Mensagem REQUISICAO NAO ENVIADA no Console: confira as variáveis indicadas ou a ação habilitada; há também um teste de configuração com falha.
- Após reiniciar o ConfApi com o código atualizado, procure Rodoviario entrada/saida nos logs INFO. A saída inclui status e duracaoMs; o header X-Rodoviario-Request-Id relaciona a resposta aos logs de integração HUB/MANAGER. Essas linhas não imprimem query strings, tokens ou corpos. Etapa autenticacao sem envio indica espera no ConfApp; etapa envio indica espera no Hub/Manager.
- Sem log de entrada após clicar Send: confirme que a requisição foi enviada no Console, a porta 8088, o contexto do WAR e a versão em execução. Teste 31 sem Bearer deve receber 401/403 mesmo antes do controller.
- 404 geral: confira contexto do WAR, prefixo e versões implantadas.
- `404 Reserva nao encontrada` na busca `/reservaRodoviario/findByLocalizador/...`: resultado esperado quando a compra existe no Hub e ainda nao foi gravada no Manager. O ConfApi segue para conferir vinculos e chamar `salvarCompleta`; esse 404 nao encerra a recuperacao.
- `VINCULOS_DESTINO_INVALIDOS`: os IDs informados em `dadosManager` nao foram encontrados no catalogo de vinculos ativos do sistema. A resposta identifica o sistema e os IDs ausentes. Na pesquisa, `origem`/`destino` recebem `codgCidade`; na reserva, `dadosManager.trechos[].trecho.codgCidadeOrigem/codgCidadeDestino` recebem `codgVinculoCidadeSistema`. Confira o cadastro dos terminais da compra; vinculos de cidade `CIT_...` nao substituem automaticamente os terminais `ROD_...` retornados pelo fornecedor.
- `CONFERENCIA_NECESSARIA` com origem/destino divergentes: compare os codigos `origem.codigoFornecedor` e `destino.codigoFornecedor` da compra com `codgDestinoSistema` dos vinculos ativos no mesmo `codgSistema`. Os campos `dadosManager.trechos[].trecho.codgCidadeOrigem` e `codgCidadeDestino` recebem `codgVinculoCidadeSistema`, nao o ID da cidade nem o numero apos ROD_/CIT_. Corrija esses IDs tambem dentro de `payloadRecuperacao`: alterar apenas as variaveis do environment nao modifica a snapshot ja salva. Mantenha a chave original e recupere pela requisicao 17. A resposta mostra os IDs/codigos divergentes sem dados pessoais.
- `SocketException` no transporte do Manager: interrupcao de conexao, sem resposta HTTP. Uma consulta GET sem efeito pode ser repetida uma unica vez. O log indica motivo classificado e tentativa, sem imprimir a mensagem original. Nao ha repeticao automatica de reserva, emissao, gravacao, timeout, conexao recusada ou respostas HTTP de erro. Falha persistente exige verificar o processo/log do Manager no mesmo horario e recuperar a compra pela requisicao 17.
- 502/503/504: confira servico e codigo na resposta, URLs e credenciais internas. Havendo efeito incerto, recupere por consulta.

Nenhuma chamada real é executada na geração deste pacote. JSONs e scripts são verificados offline; credenciais, disponibilidade, pagamentos e estados precisam de homologação. Tokens e senhas começam vazios. Não exporte dados pessoais, senhas, tokens ou snapshots preenchidas para o repositório ou terceiros.

## Reserva interrompida antes do backend

Se o Console mostrar `REQUISICAO NAO ENVIADA`, nao existe uma chamada correspondente no ConfApi. A requisicao 14 nao exige mais `permitirOperacoesComEfeito` nem `acaoPermitida`; ela mantem as validacoes dos campos obrigatorios e do JSON. Se o aviso antigo de habilitacao ainda aparecer, reimporte a collection atualizada ou substitua todo o conteudo de **Scripts > Pre-request** pelo arquivo `reservar-manual.pre-request.js` e salve. Alterar o arquivo do repositorio nao atualiza uma requisicao ja importada no Postman. As demais operacoes com efeito continuam exigindo habilitacao.

Para erros que chegarem ao servidor, procure `Rodoviario erro` nos logs. A linha inclui requestId, etapa, servico, status, codigo, tipo, requerConferencia, motivo local e origem (classe/metodo/linha). Falhas parciais tambem sao registradas. Corpos, tokens e mensagens arbitrarias recebidas de terceiros nao sao impressos. O header `X-Rodoviario-Request-Id` permite relacionar a resposta aos logs.

## Logs da gravacao no Manager

O endpoint `POST /reservaRodoviario/salvarCompleta` do Manager registra `Rodoviario Manager gravacao request` com o JSON recebido e `gravacao response` com status 201 e o JSON retornado. Em caso de falha, `gravacao erro` informa status, motivo da validacao, tipos das causas, SQLState/codigo SQL quando disponiveis e pontos do codigo. Nao ha nova tentativa de gravacao por causa do log.

O ConfApi encaminha `X-Rodoviario-Request-Id`, permitindo procurar o mesmo `requestId` nos dois consoles. IDs, localizador, trechos e valores permanecem visiveis; dados pessoais sao mascarados e tokens nao sao registrados. O mascaramento afeta apenas a copia usada no log. Corpos acima de 32 KiB sao truncados.

Se o ConfApi retornar `VINCULOS_DESTINO_INVALIDOS` antes de chamar `salvarCompleta`, nao existira log de request/response da gravacao no Manager. O `404 Reserva nao encontrada` de `findByLocalizador` e uma consulta previa, e nao uma falha ao inserir a reserva.

## Alternativa: envio manual da reserva

Voce tambem pode importar apenas `confapi-rodoviario-reserva-manual.postman_collection.json`. Abra a nova collection **ConfApi Rodoviario - Reserva manual**, selecione o environment ja preenchido e use sua unica requisicao. Assim como a requisicao 14 atualizada, esta alternativa nao depende de `permitirOperacoesComEfeito` nem de `acaoPermitida`. Body, Bearer appToken, X-Usuario-Token, validacao de campos e snapshot para recuperacao continuam iguais.

Para corrigir diretamente a requisicao ja aberta, substitua todo o conteudo de **Scripts > Pre-request** pelo arquivo `reservar-manual.pre-request.js` e salve a requisicao. Editar arquivos do repositorio nao atualiza automaticamente uma requisicao ja importada no Postman.

O envio manual executa booking e checkout ao clicar Send. Execute individualmente, sem Runner em lote. Se uma tentativa anterior chegou ao fornecedor e ficou incerta, consulte/sincronize a compra antes de enviar outra reserva.
