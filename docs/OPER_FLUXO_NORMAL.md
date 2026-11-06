# OPER no fluxo normal de reserva e pacote

OPER utiliza as rotas existentes: Portal JSF → ConfApi /v2/aereo/tarifar e /reservar → Hub /api/aereo/tarifar e /reservar. Não existe contratação paralela, ledger oper_contratacao ou coluna dados_operadora na reserva aérea.

A identificação persistida permanece em reserva_aereo.tipo_tarifa, com CONS ou OPER. A única alteração nova de esquema autorizada nesta adaptação é reserva_valor.valor_comissao, independente do principal.

## Valores e comissão

O Hub usa preços reais por faixa ADT/CHD/INF. Tarifa de venda, NET, markup, embarque, serviço, fee, RAV, combustível e assento mantêm seus campos normais. O total é agregado pelos valores unitários e quantidades reais; não se divide um preço RT agregado arbitrariamente entre passageiros ou sentidos.

valorComissao é decimal e informacional. Não deve ser somado ao principal, nem reutilizar RC, RAV ou markup para representar comissão. O Portal persiste os valores retornados na reserva normal e preserva a modalidade.

A seleção RT é conjugada: um Trecho com ida e volta, preço RT único e metadados reais de cabine/família/classe por voo. Não se altera uma rota nacional para internacional para encaixar o resultado.

## Validação e autenticação

Requests normais acrescentam tipoTarifa, criteriosOperadora, chaveSelecao, codgPacote, codgReservaHotel e precoEsperadoOperadora. Critérios e chave permitem ao Hub resolver novamente a oferta e suas referências no servidor. O preço esperado serve apenas para comparação; nunca autoriza o cliente a definir o preço.

Somente OPER exige X-Usuario-Token individual no encaminhamento normal. A ConfApi continua usando seu Bearer técnico para chamar o Hub. O Hub deriva o perfil configurado e valida passageiros, preço e hotel confirmado do mesmo pacote antes de reservar. CONS conserva o caminho anterior.

No pacote OPER, o confirmador existente executa hotel antes do aéreo no mesmo fluxo. Não se exige que os hóspedes sejam todos os passageiros nem que a hospedagem cubra toda a estadia.

## Limites operacionais

Não há novo ledger de tentativas. Os mecanismos atuais do sistema controlam processamento e localizadores; não há retry automático do POST de reserva. Se o fornecedor não confirmar a resposta, conferir a reserva e os localizadores antes de qualquer nova solicitação.

Sem persistência adicional de perfil por reserva, a operação depende de uma conta OPER configurada de forma unívoca. Não trocar a conta associada a localizadores ativos. Rotação do segredo da mesma conta não altera essa identidade; ambiguidades devem ser recusadas, sem cair em credencial CONS.

## Implantação

Revisar e aplicar somente a migração da comissão em reserva_valor antes de publicar o Manager que lê essa coluna. O DDL anterior de ledger/snapshot foi retirado dos fontes e não foi executado nesta tarefa. Não há DROP ou remoção de dados de banco.

Publicar versões compatíveis de Portal, ConfApi, Hub e Manager. Validação local utiliza testes e transportes simulados; homologação com Wooba e pagamento real deve ocorrer no ambiente apropriado.
