# Prompt para gerar JSON de regras aereas de alteracao/remarcacao

Use este prompt no ChatGPT com navegacao habilitada.

```text
Voce e um analista de regras tarifarias aereas para uma agencia de viagens corporativa.

Sua tarefa e acessar APENAS as fontes oficiais abaixo, extrair as regras de ALTERACAO, REMARCACAO, REEMISSAO e NO-SHOW das companhias GOL, LATAM e AZUL, e devolver um JSON unico, padronizado e importavel no banco de dados.

Fontes oficiais:
- GOL: https://www.voegol.com.br/nh/informacoes/cancelamento-e-reembolso
- LATAM: https://www.latamtrade.com/pt_br/procom/tarifas-pt-2/atributos-brands-dombr
- AZUL: https://www.voeazul.com.br/br/pt/sua-viagem/alteracao-e-cancelamento

IMPORTANTE:
1. Leia as paginas oficiais antes de preencher os dados.
2. Nao invente valores, percentuais, multas, familias, classes ou condicoes.
3. Quando uma informacao nao estiver clara na fonte, preencha o campo numerico como null, marque requer_validacao_manual=true no item, use status_revisao="PENDENTE_REVISAO" e explique em observacao.
4. Nao retorne texto explicativo fora do JSON.
5. Nao use markdown.
6. Nao use comentarios no JSON.
7. Use somente JSON valido.
8. Todos os valores monetarios devem ser numeros decimais, sem "R$".
9. Percentuais devem ser numeros decimais, sem "%". Exemplo: 10% vira 10.00.
10. Cada familia tarifaria deve gerar regras por momento quando aplicavel:
    - ANTES_EMBARQUE
    - APOS_EMBARQUE
    - NO_SHOW
11. Se a regra for igual para mais de um momento, repita os itens, um por momento.
12. Sempre preencha fonte_url e data_consulta em cada item.
13. Use status_revisao="PENDENTE_REVISAO" em todos os itens.
14. Use origem_regra="IA_SITE_OFICIAL" em todos os itens.
15. Use ativo=true em todos os itens validos.
16. Para esta primeira fase, preencha tipo_evento="REMARCACAO" em todos os itens, pois o sistema consulta a regra de alteracao/remarcacao usando esse tipo_evento.

Objetivo do JSON:
- Popular a tabela regra_aerea_alteracao.
- Permitir que o sistema, ao carregar uma reserva, identifique pela companhia, mercado, familia tarifaria e classe se a reserva permite alteracao/remarcacao.
- Informar de forma clara se ha multa, se cobra diferenca tarifaria e qual a previa da multa quando houver valor fixo ou percentual.

Relacionamento com familia_companhia:
- codg_familia_companhia deve ser null quando voce nao souber o codigo interno do banco.
- companhia, mercado, familia_tarifaria, familia_tarifaria_normalizada e codigo_tarifario sao campos de apoio para o importador localizar a familia_companhia cadastrada.
- A tabela principal usa codg_familia_companhia quando a regra for por familia.
- A tabela principal usa codg_companhia_aerea quando a regra for geral da companhia ou baseada somente em classe, sem familia especifica.
- Quando a regra for independente da familia e depender somente da classe, use codg_familia_companhia=null, preencha companhia e classes_reserva/classes_reserva_json.

Enums e padroes permitidos:

companhia:
- GOL
- LATAM
- AZUL

mercado:
- NACIONAL
- INTERNACIONAL
- AMBOS
- NAO_INFORMADO

tipo_evento:
- REMARCACAO

momento:
- ANTES_EMBARQUE
- APOS_EMBARQUE
- NO_SHOW
- QUALQUER_MOMENTO

status_alteracao:
- PERMITIDA_SEM_MULTA
- PERMITIDA_COM_MULTA
- NAO_PERMITIDA
- VALIDACAO_MANUAL
- NAO_INFORMADO

tipo_alteracao:
- REMARCACAO
- ALTERACAO_DATA
- ALTERACAO_VOO
- ALTERACAO_ROTA
- REEMISSAO
- VALIDACAO_MANUAL
- NAO_INFORMADO

tipo_multa:
- SEM_MULTA
- VALOR_FIXO
- PERCENTUAL
- MENOR_ENTRE_FIXO_E_PERCENTUAL
- MAIOR_ENTRE_FIXO_E_PERCENTUAL
- VARIAVEL
- NAO_INFORMADO

criterio_multa:
- POR_PASSAGEIRO
- POR_TRECHO
- POR_PASSAGEIRO_E_TRECHO
- POR_RESERVA
- NAO_INFORMADO

criterio_multiplos_trechos:
- COBRA_POR_TRECHO_ALTERADO
- COBRA_UMA_VEZ
- COBRA_MAIOR_VALOR
- NAO_INFORMADO

base_calculo_multa:
- TARIFA
- TOTAL_RESERVA
- VALOR_NOVA_TARIFA
- POR_PASSAGEIRO
- POR_TRECHO
- NAO_INFORMADO

tipo_reemissao:
- COM_REEMISSAO
- SEM_REEMISSAO
- VALIDACAO_MANUAL
- NAO_INFORMADO

novo_valor_minimo:
- IGUAL_OU_MAIOR
- PODE_SER_MENOR
- NAO_INFORMADO

Regras de preenchimento:

- chave_regra e um campo tecnico de importacao. Crie um valor unico e estavel usando:
  COMPANHIA|MERCADO|FAMILIA|CODIGO|CLASSE|TIPO_EVENTO|MOMENTO

- codg_companhia_aerea deve ser null quando voce nao souber o codigo interno do banco.
- codg_familia_companhia deve ser null quando voce nao souber o codigo interno do banco.
- familia_tarifaria deve manter o nome comercial encontrado na fonte.
- familia_tarifaria_normalizada deve ser o nome da familia em maiusculo, sem acentos, usando "_" como separador. Exemplo: "Mais Azul" vira "MAIS_AZUL".
- codigo_tarifario deve ser preenchido quando a fonte trouxer codigo, brand, sigla, familia codigo ou base tarifaria.
- classes_reserva deve ser texto separado por virgula quando houver classes explicitas. Exemplo: "V,UU,X,W,OL,OO,Z".
- classes_reserva_json deve ser array de strings quando houver classes explicitas; senao null.

- permite_alteracao=true somente quando a fonte informar que alteracao/remarcacao e permitida.
- permite_alteracao=false quando a fonte informar que a tarifa nao permite alteracao/remarcacao ou quando a condicao for no-show nao permitido.

- cobra_diferenca_tarifaria=true quando a fonte mencionar diferenca tarifaria, diferenca de tarifa, novo valor da tarifa, ou regra equivalente.
- cobra_diferenca_tarifaria=false somente quando a fonte declarar claramente que nao cobra diferenca tarifaria.
- Se a fonte nao informar, use cobra_diferenca_tarifaria=true e observacao explicando que a validacao operacional deve confirmar.

- aplica_multa=true quando houver taxa, multa, penalidade, fee ou cobranca para alterar/remarcar.
- aplica_multa=false quando a fonte informar sem multa ou quando alteracao nao for permitida.
- valor_multa_fixo deve ser preenchido somente quando a fonte trouxer valor fixo.
- valor_multa_fixo_europa deve ser preenchido somente quando a fonte trouxer valor especifico para Europa.
- valor_multa_fixo_demais_internacionais deve ser preenchido somente quando a fonte trouxer valor especifico para demais internacionais.
- percentual_multa deve ser preenchido somente quando a fonte trouxer percentual de multa.

- por_passageiro=true quando a fonte informar cobranca por passageiro ou quando a regra de mercado normalmente for por passageiro e a fonte nao negar isso.
- por_trecho=true quando a fonte informar cobranca por trecho, segmento ou trecho alterado.
- Se a fonte nao deixar claro se e por passageiro/trecho, marque true para por_passageiro, null ou false para por_trecho conforme a evidencia, e explique em observacao.

- permite_no_show=true somente quando a fonte informar que ainda permite alteracao/remarcacao apos no-show.
- permite_no_show=false quando a regra de no-show nao permitir alteracao/remarcacao, ou quando o bilhete perde validade para essa acao.

- exige_reemissao=true quando a alteracao gerar novo bilhete, reemissao ou recalculo tarifario.
- tipo_reemissao deve resumir se exige reemissao.

- permite_alterar_rota=true somente se a fonte permitir mudanca de origem/destino/rota.
- permite_alterar_cabine=true somente se a fonte permitir mudanca de cabine.
- permite_alterar_operadora=true somente se a fonte permitir mudanca de companhia operadora.
- permite_mudar_dom_int=true somente se a fonte permitir mudar domestico para internacional ou internacional para domestico.
- Quando a fonte nao informar esses itens, use null e explique em observacao quando for relevante.

- titulo_usuario deve ser curto e claro para usuario leigo.
  Exemplos:
  "Remarcacao permitida sem multa"
  "Remarcacao permitida com multa"
  "Remarcacao nao permitida"

- descricao_usuario deve explicar a regra em linguagem simples.
  Exemplo: "A tarifa permite alterar o voo antes do embarque. Pode haver diferenca tarifaria conforme o novo voo escolhido."

- mensagem_calculo deve explicar como calcular a previa.
  Exemplo: "Previa considera multa de alteracao + diferenca tarifaria quando a nova tarifa for informada."

- orientacao_interna deve trazer alerta operacional para equipe.
  Exemplo: "Validar disponibilidade, diferenca tarifaria e regra de no-show antes de confirmar com o cliente."

- observacao deve trazer um resumo curto da evidencia usada, incluindo familia_origem, tabela_origem e observacoes relevantes. Nao copie textos longos da fonte.

Formato obrigatorio de saida:

{
  "schema": "regras_aereas_alteracao",
  "versao_schema": "1.0",
  "data_geracao": "YYYY-MM-DDTHH:mm:ss-04:00",
  "fontes": [
    {
      "companhia": "GOL",
      "url": "https://www.voegol.com.br/nh/informacoes/cancelamento-e-reembolso",
      "status_leitura": "LIDA",
      "data_consulta": "YYYY-MM-DDTHH:mm:ss-04:00",
      "observacao": null
    }
  ],
  "alteracoes": [
    {
      "chave_regra": "GOL|NACIONAL|FLEX|null|null|REMARCACAO|ANTES_EMBARQUE",
      "codg_companhia_aerea": null,
      "codg_familia_companhia": null,
      "companhia": "GOL",
      "mercado": "NACIONAL",
      "familia_tarifaria": "Flex",
      "familia_tarifaria_normalizada": "FLEX",
      "codigo_tarifario": null,
      "classes_reserva": null,
      "classes_reserva_json": null,
      "tipo_evento": "REMARCACAO",
      "momento": "ANTES_EMBARQUE",
      "permite_alteracao": true,
      "status_alteracao": "PERMITIDA_SEM_MULTA",
      "tipo_alteracao": "REMARCACAO",
      "cobra_diferenca_tarifaria": true,
      "aplica_multa": false,
      "tipo_multa": "SEM_MULTA",
      "criterio_multa": "NAO_INFORMADO",
      "criterio_multiplos_trechos": "NAO_INFORMADO",
      "moeda_multa": "BRL",
      "valor_multa_fixo": null,
      "valor_multa_fixo_europa": null,
      "valor_multa_fixo_demais_internacionais": null,
      "percentual_multa": null,
      "base_calculo_multa": "NAO_INFORMADO",
      "por_passageiro": true,
      "por_trecho": true,
      "permite_no_show": false,
      "exige_reemissao": true,
      "tipo_reemissao": "COM_REEMISSAO",
      "permite_alterar_rota": null,
      "permite_alterar_cabine": null,
      "permite_alterar_operadora": null,
      "permite_mudar_dom_int": null,
      "novo_valor_minimo": "IGUAL_OU_MAIOR",
      "titulo_usuario": "Remarcacao permitida sem multa",
      "descricao_usuario": "A tarifa permite remarcar antes do voo. Pode haver diferenca tarifaria conforme o novo voo escolhido.",
      "mensagem_calculo": "Previa considera multa de alteracao + diferenca tarifaria quando a nova tarifa for informada.",
      "orientacao_interna": "Validar disponibilidade e diferenca tarifaria antes de confirmar com o cliente.",
      "observacao": "Resumo curto da evidencia encontrada na fonte oficial.",
      "fonte_url": "https://www.voegol.com.br/nh/informacoes/cancelamento-e-reembolso",
      "data_consulta": "YYYY-MM-DDTHH:mm:ss-04:00",
      "prioridade": 100,
      "origem_regra": "IA_SITE_OFICIAL",
      "status_revisao": "PENDENTE_REVISAO",
      "ativo": true,
      "requer_validacao_manual": false
    }
  ],
  "pendencias_revisao": [
    {
      "companhia": "AZUL",
      "assunto": "Campo nao encontrado ou regra ambigua",
      "descricao": "Explique exatamente o que nao foi possivel confirmar na fonte oficial.",
      "fonte_url": "https://www.voeazul.com.br/br/pt/sua-viagem/alteracao-e-cancelamento"
    }
  ]
}

Antes de finalizar, valide internamente:
- O JSON esta parseavel.
- Todos os itens de alteracoes tem chave_regra unica.
- Nao existem valores monetarios como texto.
- Nao existem percentuais com simbolo %.
- Todas as regras possuem fonte_url e data_consulta.
- Todas as regras possuem tipo_evento="REMARCACAO".
- Todas as regras possuem status_revisao="PENDENTE_REVISAO".
- Todas as regras possuem origem_regra="IA_SITE_OFICIAL".
- Quando uma informacao foi inferida ou ficou ambigua, o item tem requer_validacao_manual=true e observacao explicando a pendencia.

Agora gere o JSON completo para as 3 companhias.
```
