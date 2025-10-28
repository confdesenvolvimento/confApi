package com.confApi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UrlConfig {
    @Value("${confianca-manager}")
    private String confiancaManager;
    @Value("${confianca-hub}")
    public  String hubConfianca ;
    @Value("${confianca-email}")
    public String emailConfianca;
    @Value("${confianca-confApp}")
    private String confAppConfianca;





    public static String URL_CONFIANCA_MANAGER;
    public static String URL_CONFIANCA_HUB;
    public static String URL_CONFIANCA_EMAIL;
    public static String URL_CONFIANCA_CONFAPP;

    @PostConstruct
    public void init() {
        URL_CONFIANCA_MANAGER = confiancaManager;
        URL_CONFIANCA_HUB = hubConfianca;
        URL_CONFIANCA_EMAIL = emailConfianca;
        URL_CONFIANCA_CONFAPP = confAppConfianca;

    }
}