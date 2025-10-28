package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import com.confApi.db.confManager.db.wooba.checkin.CheckinService;
import com.confApi.db.confManager.db.wooba.checkin.dto.Checkin;
import com.confApi.db.confManager.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.confManager.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.db.confManager.db.wooba.checkin.dto.ia.CheckinIAResponse;
import com.confApi.db.confManager.db.wooba.checkin.dto.ia.ReservaCheckInIA;
import com.confApi.db.confManager.db.wooba.vendas.TurVendasService;
import com.confApi.db.confManager.db.wooba.vendas.dto.RQConsultaVendasDto;
import com.confApi.db.confManager.db.wooba.vendas.dto.VendaAereaExibicaoResponse;
import com.confApi.db.confManager.db.wooba.vendas.dto.VendasAereasExibicao;
import com.confApi.db.confManager.db.wooba.vendas.dto.VendasAereasExibicaoIA;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaIA;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.confApi.db.confManager.faturas.dto.model.FaturaResponseIA;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// ChatService.java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final OkHttpClient client;
    private final OpenAIProperties props;
    private final ToolRouter tools;

    private final ChatMemoriaService chatMemoriaService;
    private final LimitesService limitesService;
    private final FaturasService faturasService;
    private final CheckinService checkinService;
    private final TurVendasService turVendasService;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    public ChatResponseDTO chat(ChatRequestDTO req) throws IOException {
        String model = Optional.ofNullable(req.model()).orElse(props.getChatModel());

        // monta body para /v1/chat/completions
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", req.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList());
        if (req.tools() != null && !req.tools().isEmpty()) {
            body.put("tools", req.tools().stream().map(td -> Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", td.name(),
                            "description", td.description(),
                            "parameters", td.jsonSchema()
                    ))).toList());
        }

        Request request = new Request.Builder()
                .url(props.getBaseUrl() + "/v1/chat/completions")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        new ObjectMapper().writeValueAsBytes(body)))
                .build();

        try (Response r = client.newCall(request).execute()) {
            String json = Objects.requireNonNull(r.body()).string();
            // parse simplificado
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode choice = root.path("choices").get(0);
            String content = choice.path("message").path("content").asText();

            // verifica se houve chamadas de ferramenta
            List<ToolCallDTO> toolCalls = new ArrayList<>();
            JsonNode tc = choice.path("message").path("tool_calls");
            if (tc.isArray()) {
                for (JsonNode n : tc) {
                    String name = n.path("function").path("name").asText();
                    String argsStr = n.path("function").path("arguments").asText("{}");
                    Map<String, Object> args = new ObjectMapper().readValue(argsStr, new TypeReference<>() {
                    });
                    Map<String, Object> result = tools.execute(name, args);
                    toolCalls.add(new ToolCallDTO(name, result));
                }
            }

            return new ChatResponseDTO(root.path("id").asText(), content, toolCalls, null);
        }
    }

    public Flux<String> stream(ChatRequestDTO req) {
        // faz chamada SSE (stream=true) e emite os deltas como texto
        return Flux.create(sink -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("model", Optional.ofNullable(req.model()).orElse(props.getChatModel()));
                body.put("stream", true);
                body.put("messages", req.messages().stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList());

                Request request = new Request.Builder()
                        .url(props.getBaseUrl() + "/v1/chat/completions")
                        .post(RequestBody.create(
                                MediaType.parse("application/json"),
                                new ObjectMapper().writeValueAsBytes(body)))
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        sink.error(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody rb = response.body()) {
                            BufferedSource src = rb.source();
                            while (!src.exhausted()) {
                                String line = src.readUtf8LineStrict();
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) break;
                                    sink.next(data);
                                }
                            }
                            sink.complete();
                        }
                    }
                });
            } catch (Exception ex) {
                sink.error(ex);
            }
        });
    }

    public void actionApis(List<ChatMessageDTO> messages, ConversationRequestDTO req) {

        List<ChatMemoria> chatMemorias = chatMemoriaService.findByBase(req.unidade());
        for (ChatMemoria chtMemoria : chatMemorias) {
            System.out.println("Memoria: " + chtMemoria.getText());
            messages.add(new ChatMessageDTO("system", "Dado do sistema: " + chtMemoria.getText()));
        }

        /*Consultar limites de credito*/
        System.out.println("Limite Erp: " + req.idErp());
        Disponibilidade limitesDisponiveis = limitesService.consultaLimiteApi(new LimiteCreditoRQ(req.idErp()));
        messages.add(new ChatMessageDTO("system", "Dado do sistema: " + limitesDisponiveis.gerarResumoLimites()));

        /* Consultar Faturas*/
        // montarMensagemFaturas(req);
        messages.add(montarMensagemFaturas(req));


        /* Consultar Boletos*/
        messages.add(montarMensagemFaturasBoleto(req));
        // montarMensagemFaturasBoleto(req);

        /*Consultar Checkin proximos 72 horas*/
        messages.add(buscarCheckinsProximos(req));

        /*Consultar Ultimas Vendas*/
        messages.add(listarUltimasVendas(req));
    }

    public ChatMessageDTO listarUltimasVendas(ConversationRequestDTO req) {
        try {
            // Monta filtro
            RQConsultaVendasDto filtro = new RQConsultaVendasDto();
            filtro.setUsuario(req.codgUsuario().intValue());

           // filtro.setUsuario(58467);
            filtro.setIsUltimasVendas(true);

            // Chama serviço e protege contra null
            List<VendasAereasExibicao> vendas = turVendasService.findVendasWoobaByParam(filtro);
            if (vendas == null) {
                vendas = java.util.Collections.emptyList();
            }

            // Normaliza sigla da LATAM (JJ -> LA)
            for (VendasAereasExibicao v : vendas) {
                String sigla = v.getSiglaCia();
                if (sigla != null && sigla.equalsIgnoreCase("JJ")) {
                    v.setSiglaCia("LA");
                }
            }
            VendaAereaExibicaoResponse fResponse = new VendaAereaExibicaoResponse();
            // Converte para IA sem serializar para String
            List<VendasAereasExibicaoIA> vendasIA = mapper.convertValue(
                    vendas,
                    new com.fasterxml.jackson.core.type.TypeReference<List<VendasAereasExibicaoIA>>() {}
            );

            // Garante lista em fResponse e adiciona resultados
            if (fResponse.getVendas() == null) {
                fResponse.setVendas(new java.util.ArrayList<>());
            }
            fResponse.getVendas().addAll(vendasIA);

            // Retorna o objeto preenchido
            return new ChatMessageDTO("system", "Dado do sistema: " + fResponse.toString());

        } catch (Exception e) {
            return new ChatMessageDTO("system", "Dado do sistema: " + "Não foi possivel listar as vendas.");
        }
    }


    public ChatMessageDTO buscarCheckinsProximos(ConversationRequestDTO req) {
        String resultadoJson;

        // 1) Busca lista no serviço (null-safe)
        List<Checkin72Horas> checkinList = Optional
                .ofNullable(checkinService.findCheckin72Horas(new CheckinRQ(req.idErp(), 2)))
                .orElseGet(java.util.Collections::emptyList);

        try {
            // 2) Converte List<Checkin72Horas> -> List<ReservaCheckInIA> sem serializar antes
            List<ReservaCheckInIA> rcIA = mapper.convertValue(
                    checkinList,
                    new com.fasterxml.jackson.core.type.TypeReference<List<ReservaCheckInIA>>() {
                    }
            );

            // 3) Monta o wrapper de resposta
            CheckinIAResponse fResponse = new CheckinIAResponse();
            fResponse.setReservaCheckInIA(
                    Optional.ofNullable(rcIA).orElseGet(java.util.ArrayList::new)
            );

            // 4) Serializa o OBJETO (não toString)
            resultadoJson = mapper.writeValueAsString(fResponse);

            System.out.println("[buscarCheckinsProximos] itens convertidos: " + fResponse.getReservaCheckInIA().size());

        } catch (Exception e) {
            System.out.println("[buscarCheckinsProximos] Erro ao montar resposta" + e);
            // fallback mínimo para não quebrar o fluxo
            resultadoJson = "{\"reservaCheckInIA\":[]}";
        }

        return new ChatMessageDTO("system", "Dado do sistema: " + resultadoJson);
    }

    public ChatMessageDTO montarMensagemFaturas(ConversationRequestDTO req) {
        // 1) Monta o request
        FaturaSicaRQ faturaSicaRQ = new FaturaSicaRQ();
        faturaSicaRQ.setInvoiceType("TODOS");
        faturaSicaRQ.setEmpfat(req.idErp());
        faturaSicaRQ.setTipoData("TODAS");
        faturaSicaRQ.setDataInicio(null);
        faturaSicaRQ.setDataFim(null);
        faturaSicaRQ.setPagamento("ABERTO");
        faturaSicaRQ.setDisabledAFaturar(false);

        // 2) Consulta o serviço
        List<FaturaSicaRS> faturas = Collections.emptyList();
        try {
            faturas = Optional.ofNullable(faturasService.faturaSica(faturaSicaRQ))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            System.out.println("Erro ao consultar faturas no faturasService " + e);
        }

        // 3) Converte/normaliza datas (dd/MM/yyyy) com null-safety
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        for (FaturaSicaRS f : faturas) {
            try {
                if (f.getDataFatura() != null && !f.getDataFatura().isBlank()) {
                    f.setConvertDataFatura(formatter.parse(f.getDataFatura()));
                }
                if (f.getDataVen() != null && !f.getDataVen().isBlank()) {
                    f.setConvertDataVen(formatter.parse(f.getDataVen()));
                }
            } catch (ParseException pe) {
                System.out.println("Falha ao parsear datas da fatura: " + pe);

            }
        }

        // 4) Prepara ObjectMapper (um só), tolerante a campos extras
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 5) Mapeia diretamente a lista de FaturaSicaRS -> List<FaturaIA> sem serializar antes
        List<FaturaIA> faturasIA = mapper.convertValue(
                faturas,
                new TypeReference<List<FaturaIA>>() {
                }
        );

        // 6) Monta o wrapper de resposta
        FaturaResponseIA fResponse = new FaturaResponseIA();
        fResponse.setFaturas(
                Optional.ofNullable(faturasIA).orElseGet(ArrayList::new)
        );

        // 7) Serializa o objeto (não use toString())
        String resultadoJson;
        try {
            resultadoJson = mapper.writeValueAsString(fResponse);
        } catch (JsonProcessingException e) {
            System.out.println("Erro serializando FaturaResponseIA " + e);

            // fallback mínimo para não quebrar o fluxo
            resultadoJson = "{\"faturas\":[]}";
        }

        // 8) Retorna já no formato de mensagem de sistema
        return new ChatMessageDTO("system", "Dado do sistema: " + resultadoJson);
    }

    public ChatMessageDTO montarMensagemFaturasBoleto(ConversationRequestDTO req) {
        // 1) Monta o request
        FaturaSicaRQ rq = new FaturaSicaRQ();
        rq.setInvoiceType("TODOS");
        rq.setEmpfat(req.idErp());
        rq.setTipoData("TODAS");
        rq.setDataInicio(null);
        rq.setDataFim(null);
        rq.setPagamento("ABERTO");
        rq.setDisabledAFaturar(false);

        // 2) Consulta serviço com fallback seguro
        List<FaturaSicaRS> faturas = Collections.emptyList();
        try {
            faturas = Optional.ofNullable(faturasService.faturaSica(rq))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            System.out.println("Erro ao consultar faturas (boletos) " + e);

        }

        // 3) Normaliza datas (dd/MM/yyyy)
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        for (FaturaSicaRS f : faturas) {
            try {
                if (f.getDataFatura() != null && !f.getDataFatura().isBlank()) {
                    f.setConvertDataFatura(formatter.parse(f.getDataFatura()));
                }
                if (f.getDataVen() != null && !f.getDataVen().isBlank()) {
                    f.setConvertDataVen(formatter.parse(f.getDataVen()));
                }
            } catch (ParseException pe) {
                System.out.println("Falha ao parsear datas:  " + pe);

            }
        }

        // 4) Remove situações indesejadas
        final Set<String> SITUACOES_REMOVER = Set.of(
                "Faturada Crédito",
                "À Faturar"
        );
        faturas.removeIf(f -> {
            String s = Optional.ofNullable(f.getSituacao()).orElse("").trim();
            // compara ignorando acentuação? Aqui, apenas case-insensitive:
            return SITUACOES_REMOVER.stream().anyMatch(x -> x.equalsIgnoreCase(s));
        });

        // 5) Mapper único e resiliente
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 6) Converte para o modelo da IA
        List<FaturaIA> faturasIA = mapper.convertValue(
                faturas,
                new TypeReference<List<FaturaIA>>() {
                }
        );

        // 7) Empacota na resposta
        FaturaResponseIA resp = new FaturaResponseIA();
        resp.setFaturas(Optional.ofNullable(faturasIA).orElseGet(ArrayList::new));

        // 8) Serializa o objeto (não usar toString())
        String json;
        try {
            json = mapper.writeValueAsString(resp);
        } catch (JsonProcessingException e) {
            System.out.println("Erro serializando FaturaResponseIA (boletos):  " + e);
            json = "{\"faturas\":[]}";
        }

        // 9) Retorna a mensagem pronta para o chat
        return new ChatMessageDTO("system", "Dado do sistema (boletos): " + json);
    }
}
