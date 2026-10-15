package com.confApi.db.confManager.rodoviario;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.rodoviario.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.MultiValueMap;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service @RequiredArgsConstructor
public class RodoviarioManagerClient {
    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    public enum Recurso {
        reservas("reservaRodoviario"),trechos("rodoviarioTrecho"),passageiros("rodoviarioPassageiro"),
        assentos("rodoviarioAssento"),bilhetes("rodoviarioBilhete"),valores("rodoviarioValor");
        public final String rota; Recurso(String rota){this.rota=rota;}
    }
    private HttpHeaders headers(String usuario,boolean individual) {
        if(individual && (usuario==null || usuario.isBlank())) throw new ResponseStatusException(UNAUTHORIZED,"X-Usuario-Token individual obrigatorio.");
        HttpHeaders h=new HttpHeaders(); h.setBearerAuth(confAppService.token().getToken()); h.setContentType(MediaType.APPLICATION_JSON);
        if(usuario!=null && !usuario.isBlank()) h.set("X-Usuario-Token",usuario); return h;
    }
    public <T> T trocar(String rota,HttpMethod metodo,Object corpo,MultiValueMap<String,String> parametros,String usuario,Class<T> tipo,boolean individual) {
        HttpHeaders h=headers(usuario,individual);
        String url=UriComponentsBuilder.fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER).pathSegment(rota.split("/")).queryParams(parametros).build().encode().toUriString();
        return restTemplate.exchange(url,metodo,new HttpEntity<>(corpo,h),tipo).getBody();
    }
    public ReservaRodoviarioDetalheDTO salvarCompleta(SalvarReservaRodoviarioRequest r,String usuario) {
        return trocar("reservaRodoviario/salvarCompleta",HttpMethod.POST,r,null,usuario,ReservaRodoviarioDetalheDTO.class,true);
    }
    public ReservaRodoviarioDetalheDTO detalhe(Integer id,String usuario) {
        return trocar("reservaRodoviario/detalhe/"+id,HttpMethod.GET,null,null,usuario,ReservaRodoviarioDetalheDTO.class,true);
    }
    public JsonNode crud(Recurso recurso,Integer id,HttpMethod metodo,Object corpo,MultiValueMap<String,String> filtros,String usuario) {
        return trocar(recurso.rota+(id==null?"":"/"+id),metodo,corpo,filtros,usuario,JsonNode.class,true);
    }
}
