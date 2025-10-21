package com.confApi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UrlConfig {
    @Value("${confianca-manager}")
    private String confiancaManager;





    public static String URL_CONFIANCA_MANAGER;


    @PostConstruct
    public void init() {
        URL_CONFIANCA_MANAGER = confiancaManager;

    }
}