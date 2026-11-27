# Integração rodoviária com o Hub e o Confiança Manager

O Hub confirma o valor do booking. O Manager registra o faturado ou processa o cartão pela Rede. Depois da confirmação do recebimento, o Hub emite a compra com `preApproved` e o Manager grava a reserva com o recebimento vinculado.

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
| POST | `/reservar` | Cria o booking, registra o recebimento, emite e grava a reserva |
| POST | `/emitir` | Confere o booking existente, registra o recebimento, emite e grava a reserva |
| POST | `/consultarReserva` ou `/consultar` | Detalhe do Manager + compra atual do Hub e sincronizacao de status |
| POST | `/cancelarReserva` ou `/cancelarcompra` | Cancelamento da compra no Hub e atualizacao do Manager quando confirmado |
| POST | `/cancelarBilhete` ou `/cancelarbilhete` | Cancelamento de um passageiro; reserva inteira so muda quando todos foram cancelados |
| POST | `/sincronizar` | Recupera a compra por referência e sincroniza a reserva existente no Manager |
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
  "recebimento": {
    "codgFormaPagamento": 1,
    "formaDePagamento": {"codgFormaPagto": 1},
    "valorPagamento": 22.40
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

Em `/reservar` e `/emitir`, preencha `recebimento` conforme `RecebimentoModel`. Use `codgFormaPagamento` igual a `formaDePagamento.codgFormaPagto`. A forma `1` é faturado, como no exemplo. A forma `2` é cartão e usa os dados de `recebimento.cartaoSelecionado`. Em `valorPagamento`, informe o valor esperado da pesquisa, positivo e com até duas casas decimais. Se informar `valorTotalNet` ou `valorTotalMkp` em `dadosManager.valores`, use o mesmo valor esperado.

Use `emissao.pagamento.metodo` igual a `preApproved` e `emissao.pagamento.parcelas` igual a `1`. O método depende da habilitação da conta no fornecedor. Em `/emitir`, informe o booking existente em `emissao.codigoReserva` e omita `reservaRodoviario`.

1. O ConfApi consulta a referência da operação no Hub. Para uma nova compra, cria o booking em `/reservar` ou consulta o booking existente em `/emitir`.
2. O total `preApproved` confirmado no booking é aplicado ao recebimento e aos totais NET e MKP. O Manager recebe o pedido em `POST /recebimento/RODOVIARIO?autorizacao=false`. O ConfApi exige confirmação do valor e do ID do recebimento e, para cartão, do TID da Rede.
3. O ConfApi emite e consulta a compra no Hub. Com os valores conciliados, grava a reserva no Manager com `recebimentoIds` e confere o vínculo no detalhe retornado.

O localizador persistido é o `codigoFornecedor` da compra. Datas, assentos, bilhetes, tarifa NET e taxas vêm da consulta do Hub. O total MKP segue o total NET confirmado pelo fornecedor. O cadastro manual de recebimento conciliado continua disponível em `/reservas/{id}/recebimentos`, com `RegistrarRecebimentoRodoviarioRequest`.

A resposta contém `success`, `mensagem`, `reserva` (detalhe do Manager), `fornecedor` (envelope do Hub), `mergeExecutado`, `statusAtualizado` e `requerConferencia`. Falhas parciais trazem `codigo` e, quando conhecido, `codigoReservaTemporaria`. Depois de uma chamada com possível efeito externo, HTTP 200 pode trazer `success: false` e `requerConferencia: true`. Em caso de cobrança inconclusiva, `PAGAMENTO_REDE_EM_CONFERENCIA` orienta a conferir o recebimento original. Uma recusa definitiva do booking preserva o código do fornecedor e retorna `requerConferencia: false`.

## Consulta e cancelamento

```json
{
  "codgReservaRodoviario": 10,
  "consulta": {"sistema": "QueroPassagem", "codigoCompra": 123456, "email": "cliente@example.com"},
  "motivo": "Solicitacao do passageiro"
}
```

`codigoCompra` pode ser omitido: usa-se o localizador salvo. Um codigo diferente e rejeitado. Cancelamento exige `motivo`; para cancelar um bilhete, inclua `consulta.codigoPassageiro` (ID do fornecedor), que e validado na compra consultada.

Confirmacao local (1) exige compra `paid` e todos os bilhetes `emitted` com numero. Compra pendente e gravada como rascunho (0); uma consulta posterior atualiza os bilhetes e confirma o Manager. Cancelamento local (2) exige compra `canceled` e todos os bilhetes `canceled`. Estados desconhecidos, contraditorios, cancelamento parcial ou pendente nao sao promovidos a confirmacao/cancelamento total.

O ConfApi considera o cancelamento concluído após o Hub confirmar o cancelamento integral e o Manager confirmar a reserva cancelada e todos os recebimentos com `status: 0`. Cancelamento parcial no Hub ou estorno pendente no Manager retorna `success: false` e `requerConferencia: true`, com o detalhe disponível da compra e da reserva.

## Recuperacao sem nova cobranca

O ConfApi calcula a referência externa a partir da agência, do sistema e da chave de idempotência. A consulta por referência ocorre antes de pedir um novo recebimento ao Manager. Reutilize a mesma chave apenas para a mesma compra.

Quando `requerConferencia` for verdadeiro, consulte o recebimento original e use `/sincronizar` com os mesmos metadados em `dadosManager`, inclusive a chave, e os mesmos `emissao.email` e `emissao.sistema`. O pedido de sincronização pode omitir `recebimento`, `emissao.pagamento` e `emissao.codigoReserva`. O endpoint procura a compra por referência e atualiza a reserva existente. Se a referência estiver ausente no Hub, retorna HTTP 409. Se houver compra no Hub sem reserva correspondente ou sem recebimento vinculado no Manager, retorna um resultado parcial para conciliação manual. Após um timeout, use a sincronização antes de qualquer nova tentativa de compra.

Os testes locais usam HTTP simulado; homologacao com os servicos implantados deve confirmar credenciais, configuracao dos sistemas/vinculos, metodos de pagamento e estados reais de retorno.

## Validacao local

```powershell
mvn -o '-Dtest=Rodoviario*Test,AppConfigTest' test
```
