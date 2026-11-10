package com.confApi.authv2;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MobileRegistration {

    @Bean
    public FilterRegistrationBean<MobileSessionFilter> mobileSessionFilterRegistration(MobileSessionFilter filter) {
        var bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
