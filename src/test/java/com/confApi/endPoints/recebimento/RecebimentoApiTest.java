package com.confApi.endPoints.recebimento;

import com.confApi.confApp.ConfAppService;
import com.confApi.db.confManager.recebimento.Recebimento;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RecebimentoApiTest {
    @Test
    void deveRejeitarIdNuloAntesDeAutenticarOuEnviarHttp() {
        RestTemplate http = mock(RestTemplate.class);
        ConfAppService auth = mock(ConfAppService.class);
        RecebimentoApi api = new RecebimentoApi(http, auth);

        assertThrows(IllegalArgumentException.class, () -> api.atualizar(null, new Recebimento()));

        verifyNoInteractions(http, auth);
    }
}