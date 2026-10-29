# Rodoviario: Hub e Confianca Manager

O fluxo segue a organizacao de carro (`CarroReservaService`): operacao no fornecedor, consulta dos dados completos, gravacao no Manager e retorno do detalhe salvo. A cobranca rodoviaria e feita pelo checkout do Hub. O endpoint legado de cobranca usado por carro nao reconhece o produto rodoviario; nao e usado para gerar uma segunda cobranca.

## Configuracao e autenticacao

As URLs usam `confianca-hub` e `confianca-manager`, existentes em cada profile. O token de aplicacao e obtido de `ConfAppService`. As credenciais do fornecedor permanecem no Hub.

O consumidor se autentica no ConfApi por `Authorization: Bearer ...`, como nos demais produtos. O ConfApi usa o token da aplicacao de `ConfAppService` para acessar Hub e Manager. Nao e necessario enviar `X-Usuario-Token`, refazer login individual ou mudar o formato dos tokens. O filtro JWT do Manager continua exigindo autenticacao da aplicacao; os endpoints rodoviarios nao acrescentam uma segunda autenticacao de usuario.

`dadosManager.reserva.codgUsuarioCriacao` e obrigatorio: informe o ID de um usuario ativo no Manager para identificar o autor, como nos dados de negocio dos demais produtos. Nao e necessario outro token. O ConfApi valida sua presenca antes de chamar o Hub; o Manager recebe esse campo no JSON, valida o usuario e grava o autor da reserva e seu historico. O banco nao permite autor nulo.

Timeouts opcionais: `rodoviario.http.connect-timeout-ms` (8000) e `rodoviario.http.request-timeout-ms` (180000). O cliente dedicado nao repete chamadas nem segue redirecionamentos.

## Rotas

Os dois prefixos `/v1/rodoviario` e `/rodoviario` sao aceitos. Os endpoints anteriores de CRUD, detalhe, destinos, historico, status e recebimentos do Manager foram preservados e tambem possuem o prefixo v1.

| Metodo | Rota apos o prefixo | Responsabilidade |
| --- | --- | --- |
| GET | `/viacoes`, `/viacoes/{codigo}` | Catalogo de viacoes do Hub |
| GET | `/localidades`, `/localidades/{codigo}` | Catalogo externo; codigos `CIT_n` ou `ROD_n` |
| GET | `/chavepagamento` | Chave publica do processador retornada pelo Hub |
| POST | `/pesquisar` ou `/pesquisa` | Pesquisa do Hub |
| POST | `/pesquisaconexoes`, `/sugestoesconexao` | Pesquisa e sugestoes de conexao do Hub |
| POST | `/mapadeassentos` ou `/mapaDeAssentos` | Mapa do Hub |
| POST | `/reservar` | Booking, checkout, consulta, gravacao e detalhe do Manager |
| POST | `/emitir` | Checkout de booking existente, consulta e gravacao no Manager |
| POST | `/consultarReserva` ou `/consultar` | Detalhe do Manager + compra atual do Hub e sincronizacao de status |
| POST | `/cancelarReserva` ou `/cancelarcompra` | Cancelamento da compra no Hub e atualizacao do Manager quando confirmado |
| POST | `/cancelarBilhete` ou `/cancelarbilhete` | Cancelamento de um passageiro; reserva inteira so muda quando todos foram cancelados |
| POST | `/sincronizar` | Recupera uma compra pela referencia, grava/atualiza o Manager, sem booking ou checkout |
| GET | `/findAllReservas` | Listagem paginada do Manager com filtros de agencia e sistema |

Pesquisa, catalogos e mapa recebem os campos do contrato do Hub e preservam o envelope `{"sistema":"QueroPassagem","dados":...}`. GET aceita `sistema` opcional. O padrao do Hub e `QueroPassagem`.

As rotas `/pesquisar`, `/pesquisa`, `/pesquisaconexoes` e `/sugestoesconexao` recebem `PesquisaRodoviarioRequestDTO`: `sistema` (padrao `QueroPassagem`), `origem`, `destino` e `dataViagem` (`LocalDate`, JSON `yyyy-MM-dd`). Origem e destino sao inteiros positivos da tabela `cidade` (`codgCidade`), devem ser diferentes, e a data deve ser hoje ou futura. O ConfApi repassa os IDs ao Hub; o adaptador QueroPassagem consulta os vinculos de cidade ativos no Manager pelo sistema configurado e converte para `CIT_...` somente antes de enviar ao fornecedor. Pesquisa nao recebe `codgVinculoCidadeSistema` nem codigos externos. Dados invalidos retornam HTTP 400 antes da chamada ao Hub. O cliente envia o DTO diretamente como JSON.

## Reserva completa

Nos endpoints `/reservar`, `/emitir` e `/sincronizar`, `dadosManager.trechos[].trecho.codgCidadeOrigem` e `codgCidadeDestino` recebem **codgCidade**, o mesmo ID da tabela cidade usado na pesquisa. O ConfApi consulta `/destinos?codgSistema=...&codgCidade=...` e seleciona o vinculo que corresponde exatamente ao terminal/cidade retornado pelo fornecedor. Somente o payload interno enviado ao Manager recebe `codgVinculoCidadeSistema`; o request original e preservado. Vinculos ou trechos ambiguos continuam exigindo conferencia. O CRUD direto do Manager e os detalhes ja salvos mantem os vinculos de persistencia. Nome, sobrenome e sexo sao metadados obrigatorios; sexo nao e inferido.

```json
{
  "reservaRodoviario": {
    "sistema": "QueroPassagem",
    "viagens": [{
      "codigoViagem": "travelId-da-pesquisa",
      "passageiros": [{
        "nome": "Maria Silva", "documento": "12345678909", "tipoDocumento": "CPF",
        "dataNascimento": "2000-01-01", "poltrona": "01"
      }]
    }]
  },
  "emissao": {
    "sistema": "QueroPassagem",
    "email": "cliente@example.com",
    "pagamento": {"metodo": "preApproved", "parcelas": 1}
  },
  "dadosManager": {
    "chaveIdempotencia": "pedido-rodoviario-0001",
    "reserva": {"codgAgencia": 7, "codgSistema": 9, "codgUsuarioCriacao": 7},
    "valores": {"valorTotalMkp": 22.40},
    "trechos": [{
      "trecho": {"codgCidadeOrigem": 11, "codgCidadeDestino": 22},
      "passageiros": [{
        "passageiro": {"nomePassageiro": "Maria", "sobrenomePassageiro": "Silva", "sexo": 1, "cpf": "12345678909"}
      }]
    }]
  }
}
```

O significado do codigo de sexo segue o cadastro do Manager. Para ida/volta e conexoes, informe metadados de cada trecho efetivo retornado na compra. Trechos repetidos com a mesma origem/destino e passageiros homonimos exigem conferencia; nao ha associacao por tentativa.

PIX/cartao usam os campos de `emissao.pagamento` e `emissao.comprador` do Hub. `preApproved` depende da habilitacao da conta no fornecedor. Em `/emitir`, informe `emissao.codigoReserva`; `reservaRodoviario` pode ser omitido.

O localizador persistido e o `codigoFornecedor` da compra. Datas, assentos, bilhetes, custo total, tarifa net e taxas vem da consulta do Hub. Metadados de venda/markup continuam no request; sem `valorTotalMkp`, usa-se o custo confirmado.

Se houver um recebimento ja conciliado, o campo opcional `recebimento` aceita o contrato de `RegistrarRecebimentoRodoviarioRequest` e e registrado pelo endpoint rodoviario especifico apos salvar a reserva. `dadosManager.recebimentoIds` permite vincular IDs preexistentes. Ambas as rotinas obedecem as restricoes de usuario interno do Manager; nao cobram nem confirmam PIX automaticamente. Uma falha nessa etapa retorna o detalhe ja salvo e `requerConferencia: true`.

## Consulta e cancelamento

```json
{
  "codgReservaRodoviario": 10,
  "consulta": {"sistema": "QueroPassagem", "codigoCompra": 123456, "email": "cliente@example.com"},
  "motivo": "Solicitacao do passageiro"
}
```

`codigoCompra` pode ser omitido: usa-se o localizador salvo. Um codigo diferente e rejeitado. Cancelamento exige `motivo`; para cancelar um bilhete, inclua `consulta.codigoPassageiro` (ID do fornecedor), que e validado na compra consultada.

A criacao segue a ordem da reserva de carro: primeiro o Hub (consulta por referencia para evitar repeticao, reserva, emissao e consulta completa), depois o Manager (busca por localizador, vinculos dos destinos, gravacao e detalhe salvo). Nao ha consulta nem gravacao no Manager antes do retorno da compra do Hub. O recebimento local previo do fluxo de carro nao e reproduzido: no rodoviario, o pagamento continua sendo realizado no checkout do Hub.

A resposta segue o formato de operacao usado em carro: `success`, `mensagem`, `reserva` (detalhe do Manager), `fornecedor` (envelope do Hub), `mergeExecutado`, `statusAtualizado` e `requerConferencia`. Uma recusa HTTP 4xx da reserva pelo Hub sem efeito incerto retorna `success=false`, `requerConferencia=false`, o `codigo` do erro e a mensagem de que nao foi possivel realizar a reserva, sem emitir nem acessar o Manager. Falhas parciais incluem `codigo` e, quando conhecido, `codigoReservaTemporaria`. Erros de entrada anteriores a qualquer efeito preservam HTTP 4xx. Depois de um efeito externo, a resposta HTTP 200 descreve o resultado parcial; o cliente deve conferir os campos de resultado. Se o Hub criou a compra mas o Manager falhou, a resposta preserva os dados do fornecedor e pede conferencia, sem repetir a emissao.

Confirmacao local (1) exige compra `paid` e todos os bilhetes `emitted` com numero. Compra pendente e gravada como rascunho (0); uma consulta posterior atualiza os bilhetes e confirma o Manager. Cancelamento local (2) exige compra `canceled` e todos os bilhetes `canceled`. Estados desconhecidos, contraditorios, cancelamento parcial ou pendente nao sao promovidos a confirmacao/cancelamento total.

O Manager atual impede editar componentes de reservas confirmadas. Cancelamentos parciais sao devolvidos em `fornecedor`, com conferencia solicitada, mantendo o estado local ate tratamento operacional. Nao ha estorno automatico de recebimentos.

## Recuperacao sem nova cobranca

Uma referencia externa estavel e calculada a partir da agencia, sistema e chave de idempotencia. Antes de um novo checkout, o ConfApi procura uma compra com essa referencia. Reutilize a mesma chave apenas para a mesma compra.

Se `requerConferencia` for verdadeiro, use `/sincronizar` com os mesmos metadados e chave. Esse endpoint somente consulta o fornecedor; se a referencia nao for localizada, retorna 409 e nao cria reserva nem compra. Nao repita `/reservar` ou `/emitir` para tentar resolver timeout. A verificacao por referencia reduz duplicidade, mas nao substitui idempotencia de checkout no fornecedor nem garante exclusao entre varias instancias da aplicacao.

Os testes locais usam HTTP simulado; homologacao com os servicos implantados deve confirmar credenciais, configuracao dos sistemas/vinculos, metodos de pagamento e estados reais de retorno.

## Validacao local

```powershell
mvn -o '-Dtest=Rodoviario*Test,AppConfigTest' test
```
