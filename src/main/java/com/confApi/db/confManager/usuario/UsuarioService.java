package com.confApi.db.confManager.usuario;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class UsuarioService  extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = UrlConfig.URL_CONFIANCA_MANAGER +"/wooba/turUsuarios";
}
