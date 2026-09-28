package com.confApi.wooba.sales;

import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.dto.WoobaManualImportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WoobaManualImportControllerTest {
    private final WoobaManualImportService service = mock(WoobaManualImportService.class);
    private final TelegramErrorAlert telegram = mock(TelegramErrorAlert.class);
    private final String url = "/api/wooba/reservas-aereas/importar";
    private final String body = "{\"localizador\":\"JTTOQR\",\"dataCriacao\":\"2026-09-23\"}";
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("api.confplus", "", List.of());

    private MockMvc mvc(boolean enabled) {
        return MockMvcBuilders.standaloneSetup(new WoobaManualImportController(service, telegram, enabled, false, "api.confplus")).build();
    }

    @Test void deveExigirAutenticacaoEAcessoTecnico() throws Exception {
        mvc(true).perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc(true).perform(post(url).principal(new UsernamePasswordAuthenticationToken("externo", "", List.of()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void deveRetornarResumoSemExporPassageirosOuPagamentos() throws Exception {
        when(service.importar(any())).thenReturn(new WoobaManualImportResponse(true, "Importada", "CRIADA", 264800,
                "JTTOQR", "G3", 1, "Reservada", 2, 0, 0));
        mvc(true).perform(post(url).principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.codgReservaAereo").value(264800)).andExpect(jsonPath("$.passageiros").value(2));
    }

    @Test void deveValidarDataIsoENaoConverterErroEmSucesso() throws Exception {
        mvc(true).perform(post(url).principal(auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"localizador\":\"JTTOQR\",\"dataCriacao\":\"23/09/2026\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.sucesso").value(false));
        when(service.importar(any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva nao encontrada."));
        mvc(true).perform(post(url).principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Reserva nao encontrada."));
    }

    @Test void deveRespeitarDesativacaoETelegramDesligado() throws Exception {
        mvc(false).perform(post(url).principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());
        verifyNoInteractions(service);
        when(service.importar(any())).thenThrow(new IllegalStateException("dado sensivel nao deve sair"));
        mvc(true).perform(post(url).principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.sucesso").value(false));
        verifyNoInteractions(telegram);
    }
}
