package com.confApi.endPoints.reservaAereo;

import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaAereoApiTest {

    @Test
    void deveConsiderarLaEJjComoMesmaCompanhia() {
        ReservaAereoApi api = new ReservaAereoApi(null);
        CompanhiaAerea latamLa = new CompanhiaAerea(1, "LA");
        CompanhiaAerea latamJj = new CompanhiaAerea(2, "JJ");

        Boolean mesmaCompanhia = ReflectionTestUtils.invokeMethod(api, "mesmaCompanhia", latamLa, latamJj);

        assertTrue(Boolean.TRUE.equals(mesmaCompanhia));
    }
}
