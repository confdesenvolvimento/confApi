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
import com.confApi.geradorPdf.seguro.EnvioReservaSeguroPDF;
import com.confApi.geradorPdf.seguro.GeradorSeguroPDFModel;
import com.confApi.geradorPdf.seguro.PlanoViagemReservaSeguroPDF;
import com.confApi.geradorPdf.seguro.ReservaSeguroModelPDF;
import com.confApi.hub.aereo.ReservaAereoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class GeradorPDFService {

    @Autowired
    private ReservaAereoService reservaAereoService;
    @Autowired
    private UsuarioService usuarioService;

    public byte[] gerarPopularAereoPDF(GeradorAereoPDFModel geradorAereoPDFModel) {
        EnvioReservaAereoPDF envioReservaAereoPDF = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        return new GeradorPDFApi().gerarAereoPDF(envioReservaAereoPDF); // 👈 retorna o byte[]
    }

    public void popularAereoPDF(GeradorAereoPDFModel geradorAereoPDFModel) {
        EnvioReservaAereoPDF envioReservaAereoPDF = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        new GeradorPDFApi().envioAereoPDF(envioReservaAereoPDF);
    }

    public void popularHotelPDF(GeradorHotelPDFModel geradorHotelPDFModel) {
      //  System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getUsuarioConfDto());
        EnvioReservaHotelPDF envioReservaHotelPDF = new EnvioReservaHotelPDF(geradorHotelPDFModel);
      //  System.out.println("reserva : "+envioReservaHotelPDF.getReservaHotelModelPDF().getLocalizador());
        new GeradorPDFApi().envioHotelPDF(envioReservaHotelPDF);
    }

    public void popularCarroPDF(GeradorCarroPDFModel geradorCarroPDFModel) {

        if (geradorCarroPDFModel == null
                || geradorCarroPDFModel.getReservaCarroModelPDF() == null) {
            return;
        }

        ReservaCarroModelPDF reservaCarroModelPDF = geradorCarroPDFModel.getReservaCarroModelPDF();

        reservaCarroModelPDF.setCompanhiaCarroName(ajustarNomeLocadora(reservaCarroModelPDF.getCompanhiaCarroName())
        );

        /*
         * Caso já tenha criado o método da logo,
         * execute depois da normalização do nome.
         */
        preencherLogoLocadora(reservaCarroModelPDF);

        EnvioReservaCarroPDF envioReservaCarroPDF = new EnvioReservaCarroPDF(geradorCarroPDFModel);

        new GeradorPDFApi().envioCarroPDF(envioReservaCarroPDF);
    }

    private void preencherLogoLocadora(ReservaCarroModelPDF reserva) {
        if (reserva == null) {
            return;
        }

        /*
         * Preserva uma logo que já tenha sido enviada pelo Front.
         */
        if (reserva.getCompanhiaCarroLogo() != null
                && !reserva
                .getCompanhiaCarroLogo()
                .trim()
                .isEmpty()) {
            return;
        }

        String logo = LocadoraLogoResolver.resolver(null, reserva.getCompanhiaCarroName());
        reserva.setCompanhiaCarroLogo(logo);
    }

    private String ajustarNomeLocadora(String nomeLocadora) {

        if (nomeLocadora == null
                || nomeLocadora.trim().isEmpty()) {
            return nomeLocadora;
        }

        String nomeAjustado = nomeLocadora.trim();

        nomeAjustado = nomeAjustado.replaceFirst("\\s*[-–|]?\\s*"
                        + "\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}"
                        + "\\s*$",
                ""
        );

        /*
         * Remove espaços duplicados que possam restar.
         */
        nomeAjustado = nomeAjustado
                .replaceAll("\\s{2,}", " ")
                .trim();

        return nomeAjustado;
    }

    public void popularSeguroPDF(GeradorSeguroPDFModel geradorSeguroPDFModel) {

        if (geradorSeguroPDFModel == null) {
            throw new IllegalArgumentException(
                    "Os dados para geração do seguro não foram informados."
            );
        }

        ReservaSeguroModelPDF reserva = geradorSeguroPDFModel.getReservaSeguroModelPDF();

        if (reserva == null) {
            throw new IllegalArgumentException(
                    "A reserva de seguro não foi informada."
            );
        }

        PlanoViagemReservaSeguroPDF plano = geradorSeguroPDFModel.getPlanoViagemReservaSeguroPDF();

        if (plano == null) {
            plano = new PlanoViagemReservaSeguroPDF();

            geradorSeguroPDFModel.setPlanoViagemReservaSeguroPDF(plano);
        }

        /*
         * Protege o projeto de e-mail contra listas nulas
         * enviadas explicitamente no JSON.
         */
        if (reserva.getSegurados() == null) {
            reserva.setSegurados(new ArrayList<>());
        }

        if (reserva.getRecebimentos() == null) {
            reserva.setRecebimentos(new ArrayList<>());
        }

        String identificador;

        if (reserva.getLocalizador() != null
                && !reserva.getLocalizador().trim().isEmpty()) {

            identificador = reserva.getLocalizador().trim();

        } else if (reserva.getCodgReservaSeguroDB() != null) {

            identificador = String.valueOf(
                    reserva.getCodgReservaSeguroDB()
            );

        } else {
            identificador = "NÃO INFORMADO";
        }

        boolean cancelada = reserva.getDataCancelamento() != null
                        || (
                        reserva.getStatusDescricao() != null
                                && "CANCELADO".equalsIgnoreCase(
                                reserva.getStatusDescricao().trim()
                        )
                )
                        || (
                        reserva.getStatusDescricao() != null
                                && "CANCELADA".equalsIgnoreCase(
                                reserva.getStatusDescricao().trim()
                        )
                );

        /*
         * Só define assunto e mensagem quando o Front
         * não tiver enviado valores próprios.
         */
        if (plano.getAssunto() == null
                || plano.getAssunto().trim().isEmpty()) {

            plano.setAssunto(cancelada
                            ? "Cancelamento Reserva Seguro - "
                            + identificador
                            : "Reserva Seguro - "
                            + identificador
            );
        }

        if (plano.getMensagem() == null
                || plano.getMensagem().trim().isEmpty()) {

            plano.setMensagem(cancelada
                            ? "Informamos que a reserva de seguro "
                            + identificador
                            + " foi cancelada."
                            : "Segue o plano de viagem da reserva "
                            + "de seguro "
                            + identificador
                            + " conforme solicitado."
            );
        }

        EnvioReservaSeguroPDF envioReservaSeguroPDF = new EnvioReservaSeguroPDF(geradorSeguroPDFModel);

        new GeradorPDFApi().envioSeguroPDF(envioReservaSeguroPDF);

    }

    public void popularAereoPDFApp(GeradorAereoPDF geradorAereoPDF) {

        UsuarioResponse usuarioResponse = usuarioService.consultarUsuario(geradorAereoPDF.getUsuarioLogin());
        usuarioResponse.setAgencia(new Agencia(geradorAereoPDF.getAgenciaCodg()));
       // System.out.println("usuarioResponse : "+usuarioResponse);

        ReservaAereoResponse reservaAereoResponse = reservaAereoService.consultarLocalizador(
                new ReservaAereoConsultarLocalizadorRequest(geradorAereoPDF, usuarioResponse.getAgencia())
        );

        GeradorAereoPDFModel geradorAereoPDFModel = new GeradorAereoPDFModel(
                new ReservaAereoModel(reservaAereoResponse),
                new UsuarioConfDto(usuarioResponse),
                new PlanoViagemReservaAereoPDF(geradorAereoPDF)
        );

        EnvioReservaAereoPDF envioReservaAereoPDF = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        new GeradorPDFApi().envioAereoPDF(envioReservaAereoPDF);
    }
}

