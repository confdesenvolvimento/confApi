package com.confApi.hoteis;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
import com.confApi.hoteis.dto.ReservaHotelCancelamentoDto;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import com.confApi.hub.hotel.dto.ReservarRequest;
import com.confApi.hub.hotel.mapper.HotelReservaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HotelServiceV2 {
    private final RestTemplate restTemplate;
    @Autowired private ConfAppService confAppService;
    @Autowired private UsuarioApi usuarioApi;
    @Autowired private HotelClient hotelClient;

    public HotelServiceV2(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public HotelReserva efetuarReserva(ReservarRequestFront req) {
        ReservarRequest request = HotelReservaMapper.toHubV2(req);
        return restTemplate.exchange(url(UrlConfig.URL_CONFIANCA_HUB, "api/hotel/efetuarReserva"),
                HttpMethod.POST, new HttpEntity<>(request, headers()), HotelReserva.class).getBody();
    }

    public ReservaHotel criarReservaDB(ReservaHotel pedido) {
        ReservaHotel criada = restTemplate.exchange(url(UrlConfig.URL_CONFIANCA_MANAGER, "reservaHotel"),
                HttpMethod.POST, new HttpEntity<>(pedido, headers()), ReservaHotel.class).getBody();
        if (criada == null || criada.getCodgReservaHotel() == null || criada.getCodgReservaHotel() <= 0
                || pedido.getLocalizador() == null || !pedido.getLocalizador().equalsIgnoreCase(criada.getLocalizador())) {
            throw new IllegalStateException("O fornecedor retornou a reserva, mas a gravação no Manager não foi confirmada. Não refaça a reserva.");
        }
        ReservaHotelModel referencia = new ReservaHotelModel();
        referencia.setCodgReservaHotelDB(criada.getCodgReservaHotel().longValue());
        referencia.setLocalizador(criada.getLocalizador());
        ReservaHotel confirmada = consultarReservaDB(referencia);
        if (!java.util.Objects.equals(criada.getStatus(), confirmada.getStatus())
                || pedido.getCodgReservaPacote() != null && (confirmada.getCodgReservaPacote() == null
                || !java.util.Objects.equals(pedido.getCodgReservaPacote().getCodgPacote(), confirmada.getCodgReservaPacote().getCodgPacote()))) {
            throw new IllegalStateException("A gravação da reserva precisa de conferência no Manager. Não refaça a reserva.");
        }
        return confirmada;
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {
        return hotelClient.carregarReserva(req);
    }

    public Boolean cancelarReserva(CancelarReservaRequestHotelFront req) {
        String resposta = hotelClient.cancelarReserva(req);
        return "Cancelado com sucesso!".equalsIgnoreCase(resposta == null ? null : resposta.trim());
    }

    public ReservaHotel consultarReservaDB(ReservaHotelModel req) {
        if (req == null || req.getCodgReservaHotelDB() == null || req.getCodgReservaHotelDB() <= 0
                || req.getCodgReservaHotelDB() > Integer.MAX_VALUE || req.getLocalizador() == null || req.getLocalizador().isBlank()) {
            throw new IllegalArgumentException("Identificação da reserva de hotel inválida.");
        }
        ReservaHotel registro = restTemplate.exchange(url(UrlConfig.URL_CONFIANCA_MANAGER,
                "reservaHotel/" + req.getCodgReservaHotelDB()), HttpMethod.GET,
                new HttpEntity<Void>(headers()), ReservaHotel.class).getBody();
        if (registro == null || !req.getCodgReservaHotelDB().equals(registro.getCodgReservaHotel() == null
                ? null : registro.getCodgReservaHotel().longValue())
                || !req.getLocalizador().equalsIgnoreCase(registro.getLocalizador())) {
            throw new IllegalStateException("O Manager não confirmou a identificação da reserva de hotel.");
        }
        return registro;
    }

    public void cancelarReservaDB(ReservaHotelModel req) {
        Usuario usuario = usuarioApi.consultaUsuarioByLogin(req.getUsuarioCancelamento());
        if (usuario == null || usuario.getCodgUsuario() == null || usuario.getCodgUsuario() <= 0)
            throw new IllegalArgumentException("Usuário de cancelamento não identificado.");
        ReservaHotelCancelamentoDto pedido = new ReservaHotelCancelamentoDto(req, usuario);
        if (pedido.getDataCancelamento() == null) pedido.setDataCancelamento(new java.util.Date());
        restTemplate.exchange(url(UrlConfig.URL_CONFIANCA_MANAGER, "reservaHotel/cancelar/" + req.getCodgReservaHotelDB()),
                HttpMethod.PUT, new HttpEntity<>(pedido, headers()), Void.class);
        if (!Integer.valueOf(2).equals(consultarReservaDB(req).getStatus())) {
            throw new IllegalStateException("O fornecedor cancelou a reserva, mas o Manager não confirmou a baixa.");
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(confAppService.token().getToken());
        return headers;
    }
    private static String url(String base, String relative) { return base.replaceAll("/+$", "") + "/" + relative; }
}
