package com.confApi.db.confManager.seguro.reserva;

import com.confApi.endPoints.seguro.reserva.SeguroReservaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroReservaService {


    @Autowired
    private SeguroReservaAPI seguroReservaAPI;

//    private static final Logger LOG = Logger.getLogger(SeguroApoliceService.class.getName());
//    private static final String API_ACTION_NAME = "seguroReserva";
//    private final RestTemplate restTemplate;
//    private final ObjectMapper mapper;
//
//    @Autowired
//    private ConfAppService confAppService;
//
//    @Autowired
//    public SeguroReservaService(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//        this.mapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//    }
//
//    private HttpHeaders defaultHeaders() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        try {
//            ConfAppResp token = confAppService.token();
//            if (token != null && token.getToken() != null) {
//                headers.setBearerAuth(token.getToken());
//            } else {
//                LOG.warning("Token de autenticação não encontrado no ConfAppService.");
//            }
//        } catch (Exception ex) {
//            LOG.log(Level.WARNING, "Falha ao obter token de autenticação do ConfAppService", ex);
//        }
//
//        return headers;
//    }
//
//
//    public List<SeguroReserva> findAll() {
//        List<SeguroReserva> seguroReserva = new ArrayList<>();
//        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findAll";
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.GET,
//                    new HttpEntity<>(defaultHeaders()),
//                    String.class
//            );
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                seguroReserva = mapper.readValue(
//                        response.getBody(),
//                        new TypeReference<List<SeguroReserva>>() {}
//                );
//            }
//        } catch (Exception ex) {
//            LOG.log(Level.SEVERE, "Erro ao consultar seguro reserva", ex);
//        }
//
//        return seguroReserva;
//    }


    public List<SeguroReserva> findAllSeguroReservas(){
        return seguroReservaAPI.findAll();
    }

    public Optional<SeguroReserva>findSeguroReservaById(Integer id){
        return seguroReservaAPI.findById(id);
    }

}
