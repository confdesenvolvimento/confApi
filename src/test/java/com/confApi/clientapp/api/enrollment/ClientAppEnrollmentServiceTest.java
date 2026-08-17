package com.confApi.clientapp.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientAppEnrollmentServiceTest {

    @ParameterizedTest
    @CsvSource({
            "'(65) 99999-1234',+5565999991234",
            "'65 9999-1234',+5565999991234",
            "'+55 65 99999-1234',+5565999991234",
            "'5565999991234',+5565999991234"
    })
    void normalizesBrazilianMobilePhonesToE164(String supplied, String expected) {
        assertThat(ClientAppEnrollmentService.normalizePhone(supplied)).isEqualTo(expected);
    }
}
