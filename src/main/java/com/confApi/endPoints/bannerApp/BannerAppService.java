package com.confApi.endPoints.bannerApp;

import com.confApi.db.confManager.bannerApp.BannerAppApi;
import com.confApi.db.confManager.bannerApp.BannerApp;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class BannerAppService {

    private static final String FTP_HOST = "192.168.171.27";
    private static final int FTP_PORT = 21;
    private static final String FTP_USER = "trustfly";
    private static final String FTP_PASS = "confianca14";

    private static final String FTP_BASE_AEREO = "/confianca.tur/arquivo/bannerapp/aereo";
    private static final String FTP_BASE_HOTEL = "/confianca.tur/arquivo/bannerapp/hotel";
    private static final String FTP_BASE_SEGURO = "/confianca.tur/arquivo/bannerapp/seguro";

    // caso exista um domínio/http para consumo do arquivo, ajuste aqui
    private static final String PUBLIC_BASE_URL = "https://cdn.westonline.tur.br";

    public BannerApp consultarBannerApp(Integer codgBannerApp) {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.findById(codgBannerApp);
    }

    public List<BannerApp> consultarBannerAppGeral() {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.findFiltros();
    }

    public List<BannerApp> consultarBannerAppAtivos() {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.findFiltrosAtivos();
    }

    @Transactional(rollbackFor = Exception.class)
    public BannerApp insert(BannerApp bannerApp, MultipartFile arquivo) throws Exception {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.salvar(bannerApp, arquivo);
    }

    public BannerApp update(BannerApp bannerApp, Integer codgBannerApp) throws JsonProcessingException {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.update(bannerApp, codgBannerApp);
    }

    public String delete(Integer codgBannerApp) throws JsonProcessingException {
        BannerAppApi bannerAppApi = new BannerAppApi();
        return bannerAppApi.delete(codgBannerApp);
    }
}
