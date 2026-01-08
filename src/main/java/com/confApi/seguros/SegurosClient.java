package com.confApi.seguros;

import com.confApi.confApp.ConfAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SegurosClient {
    private final RestTemplate restTemplate;

    private static final String API_ACTION = "api/seguro";
    @Autowired
    private ConfAppService confAppService;

    public SegurosClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
