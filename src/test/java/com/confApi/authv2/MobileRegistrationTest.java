package com.confApi.authv2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class MobileRegistrationTest {

    @Test
    void deveCarregarSemSobrescreverBeansEManterRegistroServletDesabilitado() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setAllowBeanDefinitionOverriding(false);
            context.register(MobileRegistration.class, MobileSessionFilter.class);
            context.refresh();

            var registrations = context.getBeansOfType(FilterRegistrationBean.class);
            assertEquals(1, registrations.size());
            var registration = registrations.values().iterator().next();
            assertSame(context.getBean(MobileSessionFilter.class), registration.getFilter());
            assertFalse(registration.isEnabled());
        }
    }
}
