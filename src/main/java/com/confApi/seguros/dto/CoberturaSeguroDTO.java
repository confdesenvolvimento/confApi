package com.confApi.seguros.dto;

import lombok.Data;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Locale;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Data
public class CoberturaSeguroDTO implements Serializable {

    private Integer orderDisplay = 0;
    private String nome;
    private String descricao;
    private Double valor = 0.0;
    private String moeda = "USD";

    // opcional UI
    private String icone; // ex: "pi pi-heart"
    private String obs;   // texto curto

    public String getIcone() {
        if (!isBlank(icone)) {
            return icone;
        }

        return gerarIconePorNome(nome);
    }

    public String gerarIconePorNome(String nomeCobertura) {

        if (isBlank(nomeCobertura)) {
            return "pi pi-shield";
        }

        String nomeNormalizado = normalizar(nomeCobertura);

        if (contem(nomeNormalizado, "cancelamento")
                || contem(nomeNormalizado, "interrupcao")
                || contem(nomeNormalizado, "atraso de voo")) {
            return "pi pi-calendar-times";
        }

        if (contem(nomeNormalizado, "pet")) {
            return "fa fa-paw";
        }
        if (contem(nomeNormalizado, "ausencia de sol")
                || contem(nomeNormalizado, "chuva")) {
            return "fa fa-cloud-rain";
        }
        if (contem(nomeNormalizado, "notebook")
                || contem(nomeNormalizado, "laptop")) {
            return "fa fa-laptop";
        }

        if (contem(nomeNormalizado, "juridica")
                || contem(nomeNormalizado, "juridico")
                || contem(nomeNormalizado, "fianca")
                || contem(nomeNormalizado, "legais")
                || contem(nomeNormalizado, "legal")) {
            return "fa fa-gavel";
        }

        if (contem(nomeNormalizado, "documento")) {
            return "pi pi-id-card";
        }

        if (contem(nomeNormalizado, "mensagem")
                || contem(nomeNormalizado, "urgente")) {
            return "pi pi-send";
        }

        if (contem(nomeNormalizado, "fundos")
                || contem(nomeNormalizado, "transferencia")) {
            return "pi pi-money-bill";
        }

        if (contem(nomeNormalizado, "bagagem")
                || contem(nomeNormalizado, "mala")
                || contem(nomeNormalizado, "malas")) {
            return "fa fa-suitcase";
        }

        if (contem(nomeNormalizado, "concierge")) {
            return "pi pi-star";
        }

        if (contem(nomeNormalizado, "acompanhante")) {
            return "pi pi-users";
        }

        if (contem(nomeNormalizado, "hospitalizacao")
                || contem(nomeNormalizado, "hospitalares")
                || contem(nomeNormalizado, "hospitalar")
                || contem(nomeNormalizado, "medicas")
                || contem(nomeNormalizado, "medica")) {
            return "fa fa-hospital";
        }

        if (contem(nomeNormalizado, "odontologica")
                || contem(nomeNormalizado, "odontologico")) {
            return "fa fa-tooth";
        }

        if (contem(nomeNormalizado, "farmaceutica")
                || contem(nomeNormalizado, "farmacia")) {
            return "fa fa-pills";
        }

        if (contem(nomeNormalizado, "traslado medico")) {
            return "fa fa-ambulance";
        }

        if (contem(nomeNormalizado, "traslado de corpo")
                || contem(nomeNormalizado, "corpo")) {
            return "fa fa-cross";
        }

        if (contem(nomeNormalizado, "retorno")
                || contem(nomeNormalizado, "regresso")) {
            return "pi pi-replay";
        }

        if (contem(nomeNormalizado, "prorrogacao")
                || contem(nomeNormalizado, "estadia")
                || contem(nomeNormalizado, "hospedagem")) {
            return "fa fa-hotel";
        }

        if (contem(nomeNormalizado, "morte")) {
            return "fa fa-heartbeat";
        }

        if (contem(nomeNormalizado, "invalidez")) {
            return "fa fa-wheelchair";
        }

        if (contem(nomeNormalizado, "roubo")
                || contem(nomeNormalizado, "furto")) {
            return "pi pi-lock";
        }

        if (contem(nomeNormalizado, "despesas")) {
            return "pi pi-wallet";
        }

        return "pi pi-shield";
    }

    private boolean contem(String texto, String busca) {
        return texto != null && texto.contains(busca);
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizar(String texto) {
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("\\p{M}", "");
        return normalizado.toLowerCase(Locale.ROOT).trim();
    }
}
