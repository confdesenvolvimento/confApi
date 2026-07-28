# Prompt para gerar JSON de regras aereas de reembolso

Use este prompt no ChatGPT com navegacao habilitada.

```text
Voce e um analista de regras tarifarias aereas para uma agencia de viagens corporativa.

Sua tarefa e acessar APENAS as fontes oficiais abaixo, extrair as regras de REEMBOLSO das companhias GOL, LATAM e AZUL, e devolver um JSON unico, padronizado e importavel no banco de dados.

Fontes oficiais:
- GOL: https://www.voegol.com.br/nh/informacoes/cancelamento-e-reembolso
- LATAM: https://www.latamtrade.com/pt_br/procom/tarifas-pt-2/atributos-brands-dombr
- AZUL: https://www.voeazul.com.br/br/pt/sua-viagem/alteracao-e-cancelamento

IMPORTANTE:
1. Leia as paginas oficiais antes de preencher os dados.
2. Nao invente valores, percentuais, multas, familias, classes ou condicoes.
3. Quando uma informacao nao estiver clara na fonte, preencha o campo numerico como null, marque requer_validacao_manual=true e explique em observacao.
4. Nao retorne texto explicativo fora do JSON.
5. Nao use markdown.
6. Nao use comentarios no JSON.
7. Use somente JSON valido.
8. Todos os valores monetarios devem ser numeros decimais, sem "R$".
9. Percentuais devem ser numeros decimais, sem "%". Exemplo: 10% vira 10.00.
10. Cada familia tarifaria deve gerar pelo menos 3 regras quando aplicavel:
    - ANTES_EMBARQUE
    - APOS_EMBARQUE
    - NO_SHOW
11. Se a regra for igual para mais de um momento, repita os itens, um por momento.
12. Sempre preencha fonte_url e data_consulta em cada item.
13. Use status_revisao="PENDENTE_REVISAO" em todos os itens.
14. Use origem_regra="IA_SITE_OFICIAL" em todos os itens.
15. Use ativo=true em todos os itens validos.

Enums e padroes permitidos:

companhia:
- GOL
- LATAM
- AZUL

mercado:
- NACIONAL
- INTERNACIONAL
- NAO_INFORMADO

momento:
- ANTES_EMBARQUE
- APOS_EMBARQUE
- NO_SHOW
- QUALQUER_MOMENTO

status_reembolso:
- REEMBOLSAVEL
- REEMBOLSAVEL_COM_MULTA
- SOMENTE_TAXAS
- NAO_REEMBOLSAVEL
- VALIDACAO_MANUAL
- NAO_INFORMADO

tipo_reembolso:
- TOTAL
- PARCIAL
- SOMENTE_TAXAS
- NAO_PERMITE
- VALIDACAO_MANUAL
- NAO_INFORMADO

tipo_multa:
- SEM_MULTA
- VALOR_FIXO
- PERCENTUAL
- MAIOR_VALOR_ENTRE_FIXO_E_PERCENTUAL
- VARIAVEL
- NAO_INFORMADO

base_calculo_multa:
- TARIFA
- TOTAL_RESERVA
- VALOR_REEMBOLSAVEL
- POR_PASSAGEIRO
- POR_TRECHO
- NAO_INFORMADO

Regras de preenchimento:

- chave_regra e um campo tecnico de importacao. Crie um valor unico e estavel usando:
  COMPANHIA|MERCADO|FAMILIA|CODIGO|CLASSE|MOMENTO

- codg_familia_companhia deve ser null quando voce nao souber o codigo interno do banco.
- companhia, mercado, familia_tarifaria e codigo_tarifario sao campos de apoio para o importador localizar a familia_companhia cadastrada. Eles nao sao colunas da tabela principal de regras.
- familia_tarifaria deve manter o nome comercial encontrado na fonte.

- codigo_tarifario deve ser preenchido quando a fonte trouxer codigo, brand, sigla, familia codigo ou base tarifaria.
- classes_reserva deve ser texto separado por virgula quando houver classes explicitas.
- classes_reserva_json deve ser array de strings quando houver classes explicitas; senao null.

- permite_reembolso=true somente quando a fonte informar que existe reembolso de tarifa ou valor.
- Se apenas taxas de embarque forem devolvidas:
  permite_reembolso=false
  reembolsa_tarifa=false
  reembolsa_taxa_embarque=true
  status_reembolso="SOMENTE_TAXAS"
  tipo_reembolso="SOMENTE_TAXAS"

- Se a tarifa nao for reembolsavel:
  permite_reembolso=false
  reembolsa_tarifa=false
  percentual_reembolso=0.00 quando isso estiver claro

- aplica_multa=true quando houver taxa, multa, penalidade, fee ou cobranca para reembolso.
- valor_multa_fixo deve ser preenchido somente quando a fonte trouxer valor fixo.
- percentual_multa deve ser preenchido somente quando a fonte trouxer percentual de multa.
- percentual_reembolso deve ser preenchido somente quando a fonte trouxer percentual de valor reembolsavel.

- considerar_tarifa=true quando a tarifa puder entrar no calculo de reembolso.
- considerar_taxa_embarque=true quando taxa de embarque puder ser devolvida.
- considerar_taxa_du=false, considerar_rav=false, considerar_rc=false, considerar_taxa_assento=false, considerar_taxa_bagagem=false e considerar_outras_taxas=false, exceto se a fonte oficial disser explicitamente que entra no reembolso.

- titulo_usuario deve ser curto e claro para usuario leigo.
  Exemplo: "Reembolso nao permitido" ou "Reembolso permitido com multa".

- descricao_usuario deve explicar a regra em linguagem simples, sem termos juridicos longos.

- mensagem_calculo deve explicar como calcular a previa.
  Exemplo: "Previa considera tarifa reembolsavel + taxa de embarque - multa."

- orientacao_interna deve trazer alerta operacional para equipe.

- observacao deve trazer um resumo curto da evidencia usada, incluindo familia_origem, tabela_origem e observacoes relevantes. Nao copie textos longos da fonte.

Formato obrigatorio de saida:

{
  "schema": "regras_aereas_reembolso",
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
  "reembolsos": [
    {
      "chave_regra": "GOL|NACIONAL|LIGHT|null|P|ANTES_EMBARQUE",
      "codg_familia_companhia": null,
      "companhia": "GOL",
      "mercado": "NACIONAL",
      "familia_tarifaria": "Light",
      "codigo_tarifario": null,
      "classes_reserva": null,
      "classes_reserva_json": null,
      "momento": "ANTES_EMBARQUE",
      "permite_reembolso": false,
      "status_reembolso": "NAO_REEMBOLSAVEL",
      "tipo_reembolso": "NAO_PERMITE",
      "reembolsa_tarifa": false,
      "reembolsa_taxa_embarque": true,
      "requer_simulacao": false,
      "requer_validacao_manual": false,
      "aplica_multa": false,
      "tipo_multa": "SEM_MULTA",
      "moeda_multa": "BRL",
      "valor_multa_fixo": null,
      "percentual_multa": null,
      "percentual_reembolso": 0.00,
      "base_calculo_multa": "NAO_INFORMADO",
      "por_passageiro": true,
      "por_trecho": false,
      "considerar_tarifa": true,
      "considerar_taxa_embarque": true,
      "considerar_taxa_du": false,
      "considerar_rav": false,
      "considerar_rc": false,
      "considerar_taxa_assento": false,
      "considerar_taxa_bagagem": false,
      "considerar_outras_taxas": false,
      "titulo_usuario": "Reembolso nao permitido",
      "descricao_usuario": "A tarifa nao permite reembolso da passagem. Pode haver devolucao de taxas reembolsaveis conforme regra da companhia.",
      "mensagem_calculo": "Previa considera somente taxas reembolsaveis, quando aplicavel.",
      "orientacao_interna": "Validar taxas e bilhete antes de solicitar ao departamento responsavel.",
      "observacao": null,
      "fonte_url": "https://www.voegol.com.br/nh/informacoes/cancelamento-e-reembolso",
      "data_consulta": "YYYY-MM-DDTHH:mm:ss-04:00",
      "prioridade": 100,
      "origem_regra": "IA_SITE_OFICIAL",
      "status_revisao": "PENDENTE_REVISAO",
      "ativo": true
    }
  ],
  "aliases": [
    {
      "companhia": "GOL",
      "sistema_origem": "Wooba",
      "mercado": "NACIONAL",
      "cabine": "ECONOMICA",
      "familia_origem": "Light",
      "familia_codigo_origem": null,
      "classe_reserva": null,
      "familia_tarifaria_normalizada": "LIGHT",
      "prioridade": 100,
      "observacao": "Alias gerado a partir do nome comercial encontrado na fonte.",
      "ativo": true
    }
  ],
  "valores_adicionais": [
    {
      "chave_regra": "GOL|NACIONAL|LIGHT|null|P|ANTES_EMBARQUE",
      "tipo_valor": "MULTA_FIXA",
      "canal": null,
      "mercado_regiao": null,
      "moeda": "BRL",
      "valor": null,
      "percentual": null,
      "observacao": "Use somente quando a fonte trouxer valores alternativos por canal, regiao ou moeda.",
      "ativo": true
    }
  ],
  "pendencias_revisao": [
    {
      "companhia": "LATAM",
      "assunto": "Campo nao encontrado ou regra ambigua",
      "descricao": "Explique exatamente o que nao foi possivel confirmar na fonte oficial.",
      "fonte_url": "https://www.latamtrade.com/pt_br/procom/tarifas-pt-2/atributos-brands-dombr"
    }
  ]
}

Antes de finalizar, valide internamente:
- O JSON esta parseavel.
- Todos os itens de reembolsos tem chave_regra unica.
- Nao existem valores monetarios como texto.
- Nao existem percentuais com simbolo %.
- Todas as regras possuem fonte_url e data_consulta.
- Todas as regras possuem status_revisao="PENDENTE_REVISAO".
- Todas as regras possuem origem_regra="IA_SITE_OFICIAL".
- Se uma informacao foi inferida, coloque requer_validacao_manual=true e descreva em observacao.

Agora gere o JSON completo para as 3 companhias.
```
