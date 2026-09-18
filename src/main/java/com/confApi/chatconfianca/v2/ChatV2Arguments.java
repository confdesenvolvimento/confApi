package com.confApi.chatconfianca.v2;

import com.confApi.chatgpt.tools.ToolDefinition;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Validate model output against the existing executor contracts, before any execution. */
public final class ChatV2Arguments {
    private ChatV2Arguments() {}
    public static ToolDefinition tool(ChatV2Capability c) {
        return switch(c) {
            case VOOS -> ToolSchemas.searchFlights();
            case TARIFA_IDA -> ToolSchemas.searchCheapestAirfares();
            case TARIFA_VOLTA -> ToolSchemas.searchCheapestRoundtripAirfares();
            case HOTEL -> ToolSchemas.searchHotels();
            case PACOTE -> ToolSchemas.searchCheapestPackages();
            default -> null;
        };
    }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> validar(ChatV2Plan p,ObjectMapper mapper) {
        ToolDefinition tool=tool(p.capability());
        if(tool==null)return Map.of();
        Map<String,Object> raw=new LinkedHashMap<>(p.getParametros());
        raw.keySet().retainAll(((Map<String,Object>)tool.jsonSchema().get("properties")).keySet());
        if(p.capability()==ChatV2Capability.VOOS)raw.putIfAbsent("adt",1);
        if(p.capability()==ChatV2Capability.HOTEL&&p.getParametros().containsKey("quartosJson"))try {
            raw.put("quartos",mapper.readValue(p.getParametros().get("quartosJson"),List.class));
        }catch(Exception ex){throw new IllegalArgumentException("Informe a ocupacao dos quartos e as idades das criancas.");}
        Map<String,Object> args=(Map<String,Object>)validarValor(raw,tool.jsonSchema(),"consulta");
        if(p.capability()==ChatV2Capability.HOTEL) {
            LocalDate checkin=LocalDate.parse(args.get("checkin").toString());
            LocalDate checkout=LocalDate.parse(args.get("checkout").toString());
            if(!checkout.isAfter(checkin))throw new IllegalArgumentException("A saida do hotel deve ser depois da entrada.");
            long noites=java.time.temporal.ChronoUnit.DAYS.between(checkin,checkout);
            if(args.containsKey("diarias")&&((Number)args.get("diarias")).longValue()!=noites)
                throw new IllegalArgumentException("A quantidade de diarias nao corresponde as datas. Confirme a hospedagem.");
            int hospedes=0;
            for(Map<String,Object> quarto:(List<Map<String,Object>>)args.get("quartos")) {
                List<Integer> idades=(List<Integer>)quarto.get("idadesCriancas");
                if(idades.size()!=((Number)quarto.get("criancas")).intValue()||idades.stream().anyMatch(i->i>17))
                    throw new IllegalArgumentException("Informe a idade de cada crianca (0 a 17 anos) por quarto.");
                hospedes+=((Number)quarto.get("adultos")).intValue()+idades.size();
            }
            if(hospedes!=((Number)args.get("totalHospedes")).intValue())
                throw new IllegalArgumentException("O total de hospedes difere da ocupacao dos quartos. Confirme a ocupacao.");
        }
        if(p.capability()!=ChatV2Capability.HOTEL) {
            for(String k:List.of("origem","destino")) {
                String code=Objects.toString(args.get(k),"").toUpperCase(Locale.ROOT);
                if(!code.matches("[A-Z]{3}"))throw new IllegalArgumentException("Informe a cidade ou aeroporto de "+k+".");
                args.put(k,code);
            }
            if(args.get("origem").equals(args.get("destino")))throw new IllegalArgumentException("Origem e destino sao iguais. Qual e a rota desejada?");
        }
        for(String[] pair:List.of(new String[]{"dataIda","dataVolta"},new String[]{"dataInicio","dataFim"},
                new String[]{"dataIdaInicio","dataIdaFim"},new String[]{"dataVoltaInicio","dataVoltaFim"},
                new String[]{"checkin","checkout"},new String[]{"mesIda","mesVolta"})) {
            if(args.containsKey(pair[0])&&args.containsKey(pair[1])
                && args.get(pair[0]).toString().compareTo(args.get(pair[1]).toString())>0)
                throw new IllegalArgumentException("O periodo informado esta invertido. Confirme as datas de ida e volta/inicio e fim.");
        }
        if(args.containsKey("duracaoMinimaDias")&&args.containsKey("duracaoMaximaDias")
                && ((Number)args.get("duracaoMinimaDias")).intValue()>((Number)args.get("duracaoMaximaDias")).intValue())
            throw new IllegalArgumentException("Confirme a duracao minima e maxima da viagem.");
        // An exact date and a different interval must not silently compete inside the legacy parser.
        for(String prefix:List.of("dataIda","dataVolta"))if(args.containsKey(prefix)) {
            for(String suffix:List.of("Inicio","Fim"))if(args.containsKey(prefix+suffix)&&!args.get(prefix).equals(args.get(prefix+suffix)))
                throw new IllegalArgumentException("Voce deseja uma data exata ou um intervalo? Confirme o periodo.");
            args.remove(prefix+"Inicio");args.remove(prefix+"Fim");
        }
        return args;
    }
    @SuppressWarnings("unchecked")
    private static Object validarValor(Object value,Map<String,Object> schema,String field) {
        Object type=schema.get("type");
        if(type instanceof List<?> list)type=list.stream().filter(x->!"null".equals(x)).findFirst().orElse(null);
        if(value==null)throw new IllegalArgumentException("Informe "+field+" para continuar.");
        switch(Objects.toString(type,"")) {
            case "object": {
                if(!(value instanceof Map<?,?>))throw new IllegalArgumentException("Parametros invalidos para "+field+".");
                Map<String,Object> source=(Map<String,Object>)value;
                Map<String,Object> fields=(Map<String,Object>)schema.get("properties");
                Map<String,Object> result=new LinkedHashMap<>();
                for(String key:(List<String>)schema.getOrDefault("required",List.of()))
                    if(!source.containsKey(key)||source.get(key)==null||source.get(key).toString().isBlank())throw new IllegalArgumentException("Informe "+key+" para continuar.");
                for(var e:source.entrySet()) {
                    if(!fields.containsKey(e.getKey()))throw new IllegalArgumentException("Parametro nao permitido.");
                    result.put(e.getKey(),validarValor(e.getValue(),(Map<String,Object>)fields.get(e.getKey()),e.getKey()));
                }
                return result;
            }
            case "array": {
                if(!(value instanceof List<?> list)||list.size()>10||list.size()<((Number)schema.getOrDefault("minItems",0)).intValue())throw new IllegalArgumentException("Informe "+field+" corretamente.");
                return list.stream().map(v->validarValor(v,(Map<String,Object>)schema.get("items"),field)).toList();
            }
            case "integer": {
                int number;
                try {number=Integer.parseInt(value.toString());}catch(RuntimeException ex){throw new IllegalArgumentException("Informe um numero inteiro para "+field+".");}
                if(number<((Number)schema.getOrDefault("minimum",0)).intValue()||number>((Number)schema.getOrDefault("maximum",100000)).intValue())throw new IllegalArgumentException("Valor fora do intervalo permitido para "+field+".");
                return number;
            }
            case "string": {
                if(!(value instanceof String text)||text.isBlank()||text.length()>200)throw new IllegalArgumentException("Informe "+field+" corretamente.");
                if(schema.containsKey("enum")&&!((List<?>)schema.get("enum")).contains(text))throw new IllegalArgumentException("Opcao invalida para "+field+".");
                try {
                    if("date".equals(schema.get("format"))) {
                        LocalDate date=LocalDate.parse(text);
                        if(date.isBefore(LocalDate.now()))throw new IllegalArgumentException();
                    }
                    if(field.startsWith("mes")&&YearMonth.parse(text).isBefore(YearMonth.now()))throw new IllegalArgumentException();
                }catch(RuntimeException ex){throw new IllegalArgumentException("Confirme a data/mes de "+field+"; a pesquisa de viagem requer um periodo valido e futuro.");}
                if(schema.containsKey("pattern")&&!text.matches(schema.get("pattern").toString()))throw new IllegalArgumentException("Formato invalido para "+field+".");
                return text;
            }
            default: throw new IllegalArgumentException("Contrato de ferramenta desconhecido.");
        }
    }
}
