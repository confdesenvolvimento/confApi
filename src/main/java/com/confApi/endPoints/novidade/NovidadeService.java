package com.confApi.endPoints.novidade;

import com.confApi.db.confManager.novidade.Novidade;
import com.confApi.db.confManager.novidade.NovidadeApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
public class NovidadeService {

    private static final String FTP_HOST = "192.168.171.27";
    private static final int FTP_PORT = 21;
    private static final String FTP_USER = "trustfly";
    private static final String FTP_PASS = "confianca14";

    private static final String FTP_BASE_NOTICIA = "/confianca.tur/arquivo/news";
    private static final String FTP_BASE_APRENDIZAGEM = "/confianca.tur/arquivo/tutorial";
    private static final String FTP_BASE_PROMOCAO = "/confianca.tur/arquivo/promocoes";

    // caso exista um domínio/http para consumo do arquivo, ajuste aqui
    private static final String PUBLIC_BASE_URL = "https://cdn.westonline.tur.br";

    public Novidade consultarNovidade(Integer codgNovidade) {
        NovidadeApi novidadeApi = new NovidadeApi();
        return novidadeApi.findById(codgNovidade);
    }

    public List<Novidade> consultarNovidadeGeral() {
        NovidadeApi novidadeApi = new NovidadeApi();
        return novidadeApi.findFiltros();
    }

    @Transactional(rollbackFor = Exception.class)
    public Novidade insert(Novidade novidade, MultipartFile arquivo) throws Exception {
        NovidadeApi novidadeApi = new NovidadeApi();
        return novidadeApi.salvar(novidade, arquivo);
    }

    public Novidade update(Novidade novidade, Integer codgNovidade) throws JsonProcessingException {
        NovidadeApi novidadeApi = new NovidadeApi();
        return novidadeApi.update(novidade, codgNovidade);
    }

    public String delete(Integer codgNovidade) throws JsonProcessingException {
        NovidadeApi novidadeApi = new NovidadeApi();
        return novidadeApi.delete(codgNovidade);
    }
}
