package com.confApi.geradorPdf;

import com.confApi.carros.LocadoraLogoResolver;
import com.confApi.db.confManager.usuario.UsuarioService;
import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import com.confApi.endPoints.agencia.Agencia;
import com.confApi.endPoints.reservaAereo.ReservaAereoConsultarLocalizadorRequest;
import com.confApi.endPoints.reservaAereo.ReservaAereoResponse;
import com.confApi.endPoints.reservaAereo.ReservaAereoService;
import com.confApi.endPoints.usuario.UsuarioResponse;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.aereo.PlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.carro.EnvioReservaCarroPDF;
import com.confApi.geradorPdf.carro.GeradorCarroPDFModel;
import com.confApi.geradorPdf.carro.ReservaCarroModelPDF;
import com.confApi.geradorPdf.hotel.EnvioReservaHotelPDF;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;
import com.confApi.geradorPdf.rodoviario.EnvioReservaRodoviarioPDF;
import com.confApi.geradorPdf.rodoviario.GeradorRodoviarioPDFModel;
import com.confApi.geradorPdf.rodoviario.PlanoViagemReservaRodoviarioPDF;
import com.confApi.geradorPdf.rodoviario.ReservaRodoviarioModelPDF;
import com.confApi.geradorPdf.seguro.EnvioReservaSeguroPDF;
import com.confApi.geradorPdf.seguro.GeradorSeguroPDFModel;
import com.confApi.geradorPdf.seguro.PlanoViagemReservaSeguroPDF;
import com.confApi.geradorPdf.seguro.ReservaSeguroModelPDF;
import com.confApi.hub.aereo.ReservaAereoModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeradorPDFService {

    @Autowired
    private ReservaAereoService reservaAereoService;

    @Autowired
    private UsuarioService usuarioService;

    public byte[] gerarPopularAereoPDF(GeradorAereoPDFModel modelo) {
        return new GeradorPDFApi().gerarAereoPDF(new EnvioReservaAereoPDF(modelo));
    }

    public void popularAereoPDF(GeradorAereoPDFModel modelo) {
        new GeradorPDFApi().envioAereoPDF(new EnvioReservaAereoPDF(modelo));
    }

    public void popularHotelPDF(GeradorHotelPDFModel modelo) {
        new GeradorPDFApi().envioHotelPDF(new EnvioReservaHotelPDF(modelo));
    }

    public void popularCarroPDF(GeradorCarroPDFModel modelo) {
        if (modelo == null || modelo.getReservaCarroModelPDF() == null) {
            return;
        }
        ReservaCarroModelPDF reserva = modelo.getReservaCarroModelPDF();
        reserva.setCompanhiaCarroName(ajustarNomeLocadora(reserva.getCompanhiaCarroName()));
        preencherLogoLocadora(reserva);
        new GeradorPDFApi().envioCarroPDF(new EnvioReservaCarroPDF(modelo));
    }

    private void preencherLogoLocadora(ReservaCarroModelPDF reserva) {
        if (reserva == null) {
            return;
        }
        if (reserva.getCompanhiaCarroLogo() != null
            && !reserva.getCompanhiaCarroLogo().trim().isEmpty()) {
            return;
        }
        reserva.setCompanhiaCarroLogo(
                LocadoraLogoResolver.resolver(null, reserva.getCompanhiaCarroName()));
    }

    private String ajustarNomeLocadora(String nomeLocadora) {
        if (nomeLocadora == null || nomeLocadora.trim().isEmpty()) {
            return nomeLocadora;
        }
        return nomeLocadora.trim()
                .replaceFirst("\\s*[-–|]?\\s*\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\s*$", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public void popularSeguroPDF(GeradorSeguroPDFModel modelo) {
        if (modelo == null) {
            throw new IllegalArgumentException("Os dados para geração do seguro não foram informados.");
        }
        ReservaSeguroModelPDF reserva = modelo.getReservaSeguroModelPDF();
        if (reserva == null) {
            throw new IllegalArgumentException("A reserva de seguro não foi informada.");
        }
        PlanoViagemReservaSeguroPDF plano = modelo.getPlanoViagemReservaSeguroPDF();
        if (plano == null) {
            plano = new PlanoViagemReservaSeguroPDF();
            modelo.setPlanoViagemReservaSeguroPDF(plano);
        }
        if (reserva.getSegurados() == null) {
            reserva.setSegurados(new ArrayList<>());
        }
        if (reserva.getRecebimentos() == null) {
            reserva.setRecebimentos(new ArrayList<>());
        }
        String identificador = reserva.getLocalizador() != null
                               && !reserva.getLocalizador().trim().isEmpty()
                ? reserva.getLocalizador().trim()
                : reserva.getCodgReservaSeguroDB() != null
                ? String.valueOf(reserva.getCodgReservaSeguroDB()) : "NÃO INFORMADO";
        boolean cancelada = reserva.getDataCancelamento() != null
                            || (reserva.getStatusDescricao() != null
                                && ("CANCELADO".equalsIgnoreCase(reserva.getStatusDescricao().trim())
                                    || "CANCELADA".equalsIgnoreCase(reserva.getStatusDescricao().trim())));
        if (plano.getAssunto() == null || plano.getAssunto().trim().isEmpty()) {
            plano.setAssunto((cancelada ? "Cancelamento Reserva Seguro - " : "Reserva Seguro - ")
                             + identificador);
        }
        if (plano.getMensagem() == null || plano.getMensagem().trim().isEmpty()) {
            plano.setMensagem(cancelada
                    ? "Informamos que a reserva de seguro " + identificador + " foi cancelada."
                    : "Segue o plano de viagem da reserva de seguro " + identificador
                      + " conforme solicitado.");
        }
        new GeradorPDFApi().envioSeguroPDF(new EnvioReservaSeguroPDF(modelo));
    }

    public void popularRodoviarioPDF(GeradorRodoviarioPDFModel modelo) {
        new GeradorPDFApi().envioRodoviarioPDF(prepararRodoviarioPDF(modelo, true));
    }

    public byte[] gerarPopularRodoviarioPDF(GeradorRodoviarioPDFModel modelo) {
        return new GeradorPDFApi().gerarRodoviarioPDF(prepararRodoviarioPDF(modelo, false));
    }

    private EnvioReservaRodoviarioPDF prepararRodoviarioPDF(
            GeradorRodoviarioPDFModel modelo, boolean exigirDestinatario) {
        if (modelo == null || modelo.getReservaRodoviarioModelPDF() == null) {
            throw new IllegalArgumentException("A reserva rodoviária não foi informada.");
        }
        if (modelo.getUsuarioConfDto() == null) {
            throw new IllegalArgumentException("O usuário do envio rodoviário não foi informado.");
        }

        ReservaRodoviarioModelPDF reserva = modelo.getReservaRodoviarioModelPDF();
        if (reserva.getLocalizador() == null || reserva.getLocalizador().isBlank()) {
            throw new IllegalArgumentException("A reserva rodoviária não possui localizador.");
        }
        if (reserva.getTrechos() == null || reserva.getTrechos().isEmpty()) {
            throw new IllegalArgumentException("A reserva rodoviária não possui itinerário para o comprovante.");
        }
        if (reserva.getRecebimentos() == null) {
            reserva.setRecebimentos(new ArrayList<>());
        }
        if (reserva.getValores() == null) {
            reserva.setValores(new ReservaRodoviarioModelPDF.ValoresPDF());
        }

        PlanoViagemReservaRodoviarioPDF plano = modelo.getPlanoViagemReservaRodoviarioPDF();
        if (plano == null) {
            plano = new PlanoViagemReservaRodoviarioPDF();
            modelo.setPlanoViagemReservaRodoviarioPDF(plano);
        }
        if (exigirDestinatario) {
            normalizarDestinatariosRodoviario(plano);
        }
        if (exigirDestinatario && (plano.getEmailPara() == null || plano.getEmailPara().isBlank())) {
            throw new IllegalArgumentException("Informe o e-mail para enviar o comprovante rodoviário.");
        }

        boolean cancelada = Integer.valueOf(2).equals(reserva.getStatus())
                            || (reserva.getDataCancelamento() != null
                                && !reserva.getDataCancelamento().isBlank());
        String localizador = reserva.getLocalizador().trim();
        if (plano.getAssunto() == null || plano.getAssunto().isBlank()) {
            plano.setAssunto((cancelada ? "Cancelamento Reserva Rodoviária - "
                    : "Plano de viagem - Reserva Rodoviária ") + localizador);
        }
        if (plano.getMensagem() == null || plano.getMensagem().isBlank()) {
            plano.setMensagem(cancelada
                    ? "Informamos que a reserva rodoviária " + localizador + " foi cancelada."
                    : "Segue em anexo o plano de viagem da reserva rodoviária "
                      + localizador + ".");
        }

        return new EnvioReservaRodoviarioPDF(modelo);
    }

    private void normalizarDestinatariosRodoviario(PlanoViagemReservaRodoviarioPDF plano) {
        Map<String, String> para = new LinkedHashMap<>();
        Map<String, String> copias = new LinkedHashMap<>();
        Map<String, String> lista = new LinkedHashMap<>();
        adicionarEmailsRodoviario(para, plano.getEmailPara());
        adicionarEmailsRodoviario(copias, plano.getEmailCopia());
        adicionarEmailsRodoviario(lista, plano.getEmailsList());

        // emailsList é um campo legado: aproveita apenas endereços ainda ausentes.
        lista.forEach((chave, email) -> {
            if (!copias.containsKey(chave)) {
                para.putIfAbsent(chave, email);
            }
        });
        copias.keySet().removeAll(para.keySet());

        plano.setEmailPara(String.join(";", para.values()));
        plano.setEmailCopia(copias.isEmpty() ? null : String.join(";", copias.values()));
        plano.setEmailsList(null);
        plano.setReceberCopiaEmail(false);
    }

    private void adicionarEmailsRodoviario(Map<String, String> destino, String lista) {
        if (lista == null || lista.isBlank()) {
            return;
        }
        for (String item : lista.split("[;,]")) {
            String email = item.trim();
            if (!email.isEmpty()) {
                destino.putIfAbsent(email.toLowerCase(Locale.ROOT), email);
            }
        }
    }

    public void popularAereoPDFApp(GeradorAereoPDF geradorAereoPDF) {
        UsuarioResponse usuarioResponse = usuarioService.consultarUsuario(geradorAereoPDF.getUsuarioLogin());
        usuarioResponse.setAgencia(new Agencia(geradorAereoPDF.getAgenciaCodg()));
        ReservaAereoResponse reservaAereoResponse = reservaAereoService.consultarLocalizador(
                new ReservaAereoConsultarLocalizadorRequest(geradorAereoPDF, usuarioResponse.getAgencia()));
        GeradorAereoPDFModel modelo = new GeradorAereoPDFModel(
                new ReservaAereoModel(reservaAereoResponse),
                new UsuarioConfDto(usuarioResponse),
                new PlanoViagemReservaAereoPDF(geradorAereoPDF));
        new GeradorPDFApi().envioAereoPDF(new EnvioReservaAereoPDF(modelo));
    }
}
