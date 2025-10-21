package com.confApi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UrlConfig {
    @Value("${confianca-manager}")
    private String confiancaManager;
    @Value("${confianca-hub}")
    public static String hubConfianca ;




    public static String URL_CONFIANCA_MANAGER;
    public static String URL_CONFIANCA_HUB;

    @PostConstruct
    public void init() {
        URL_CONFIANCA_MANAGER = confiancaManager;
        URL_CONFIANCA_HUB = hubConfianca;
    }
}