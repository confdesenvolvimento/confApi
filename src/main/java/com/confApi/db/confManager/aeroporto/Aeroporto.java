package com.confApi.db.confManager.aeroporto;

import com.confApi.db.confManager.cidade.Cidade;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Aeroporto implements Serializable {
    //  @JsonIgnore

    private Integer codgAeroporto;
    private String nomeAeroporto;
    private String codigoIata;
    private Double valrTaxaEmbarque;
    private Integer categoria;
    private Integer status;
    private Cidade cidade;
    private String nomeAereoportoExibicao;

    private String latitude;
    private String longitude;

    public Aeroporto(Integer codgAeroporto, String nomeAeroporto, String codigoIata,
                     Double valrTaxaEmbarque, Integer categoria, Integer status,
                     Cidade cidade, String nomeAereoportoExibicao, String latitude,
                     String longitude) {
        this.codgAeroporto = codgAeroporto;
        this.nomeAeroporto = nomeAeroporto;
        this.codigoIata = codigoIata;
        this.valrTaxaEmbarque = valrTaxaEmbarque;
        this.categoria = categoria;
        this.status = status;
        this.cidade = cidade;
        this.nomeAereoportoExibicao = nomeAereoportoExibicao;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Aeroporto(com.confApi.hub.aereo.dto.Aeroporto aeroporto) {
        this.nomeAeroporto = aeroporto.getDescricao();
        this.codigoIata = aeroporto.getCodigoIata();
    }

    public String getNomeAereoportoExibicao() {
        StringBuilder nomeAeroportoExibicaoBuilder = new StringBuilder();

        // Nome do aeroporto
        if (nomeAeroporto != null) {
            nomeAeroportoExibicaoBuilder.append(capitalizeWords(nomeAeroporto));
        }

        // Nome da cidade e UF (se houver)
        if (cidade != null) {
            String siglaUf = (cidade.getUf() != null) ? cidade.getUf().getSiglaUf() : null;

            if (siglaUf != null) {
                if (nomeAeroportoExibicaoBuilder.length() > 0) {
                    nomeAeroportoExibicaoBuilder.append(" - ");
                }
                nomeAeroportoExibicaoBuilder.append(siglaUf);
            } else if (cidade.getNomeCidade() != null) {
                if (nomeAeroportoExibicaoBuilder.length() > 0) {
                    nomeAeroportoExibicaoBuilder.append(" - ");
                }
                nomeAeroportoExibicaoBuilder.append(capitalizeWords(cidade.getNomeCidade()));
            }
        }

        // Código IATA
        if (codigoIata != null) {
            if (nomeAeroportoExibicaoBuilder.length() > 0) {
                nomeAeroportoExibicaoBuilder.append(" (").append(codigoIata).append(")");
            }
        }

        // País
        if (cidade != null && cidade.getPais() != null && cidade.getPais().getNomePais() != null) {
            if (nomeAeroportoExibicaoBuilder.length() > 0) {
                nomeAeroportoExibicaoBuilder.append(", ");
            }
            nomeAeroportoExibicaoBuilder.append(capitalizeWords(cidade.getPais().getNomePais()));
        }

        nomeAereoportoExibicao = nomeAeroportoExibicaoBuilder.toString();
        return nomeAereoportoExibicao;
    }


    public String getNomeAereoportoExibicaoOld2() {
        StringBuilder nomeAeroportoExibicaoBuilder = new StringBuilder();

        if (nomeAeroporto != null) {
            nomeAeroportoExibicaoBuilder.append(capitalizeWords(nomeAeroporto));
        }

        if (cidade != null && cidade.getUf() != null && cidade.getUf().getSiglaUf() != null) {
            if (nomeAeroportoExibicaoBuilder.length() > 0) {
                nomeAeroportoExibicaoBuilder.append(" - ");
            }
            nomeAeroportoExibicaoBuilder.append(cidade.getUf().getSiglaUf());
        }

        if (codigoIata != null) {
            if (nomeAeroportoExibicaoBuilder.length() > 0) {
                nomeAeroportoExibicaoBuilder.append(" (");
                nomeAeroportoExibicaoBuilder.append(codigoIata).append(")");
            }
        }

        if (cidade != null && cidade.getPais() != null && cidade.getPais().getNomePais() != null) {
            if (nomeAeroportoExibicaoBuilder.length() > 0) {
                nomeAeroportoExibicaoBuilder.append(", ");
            }
            nomeAeroportoExibicaoBuilder.append(capitalizeWords(cidade.getPais().getNomePais()));
        }

        nomeAereoportoExibicao = nomeAeroportoExibicaoBuilder.toString();
        return nomeAereoportoExibicao;
    }

    public String getNomeAereoportoExibicaoOld() {
        if (nomeAeroporto != null && cidade != null) {
            String siglaUf = cidade.getUf() != null && cidade.getUf().getSiglaUf() != null
                    ? cidade.getUf().getSiglaUf()
                    : "";
            String nomePais = cidade.getPais() != null && cidade.getPais().getNomePais() != null
                    ? capitalizeWords(cidade.getPais().getNomePais())
                    : "";

            nomeAereoportoExibicao = capitalizeWords(nomeAeroporto)
                    + " - " + siglaUf
                    + " (" + (codigoIata != null ? codigoIata : "") + "), "
                    + nomePais;
        }

        return nomeAereoportoExibicao;
    }


    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return capitalized.toString().trim();
    }

    public void setNomeAereoportoExibicao(String nomeAereoportoExibicao) {
        this.nomeAereoportoExibicao = nomeAereoportoExibicao;
    }

    public Aeroporto(Integer codgAeroporto) {
        this.codgAeroporto = codgAeroporto;
    }

    public Aeroporto(String codigoIata) {
        this.codigoIata = codigoIata;
    }

    public Aeroporto(String nomeAeroporto, String codigoIata) {
        this.nomeAeroporto = nomeAeroporto;
        this.codigoIata = codigoIata;
    }

    public Aeroporto() {
    }

    public Integer getCodgAeroporto() {
        return codgAeroporto;
    }

    public void setCodgAeroporto(Integer codgAeroporto) {
        this.codgAeroporto = codgAeroporto;
    }

    public String getNomeAeroporto() {
        return nomeAeroporto;
    }

    public void setNomeAeroporto(String nomeAeroporto) {
        this.nomeAeroporto = nomeAeroporto;
    }

    public String getIataAeroporto() {
        return codigoIata;
    }

    public void setIataAeroporto(String codigoIata) {
        this.codigoIata = codigoIata;
    }

    public Double getValrTaxaEmbarque() {
        return valrTaxaEmbarque;
    }

    public void setValrTaxaEmbarque(Double valrTaxaEmbarque) {
        this.valrTaxaEmbarque = valrTaxaEmbarque;
    }

    public Integer getCategoria() {
        return categoria;
    }

    public void setCategoria(Integer categoria) {
        this.categoria = categoria;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Cidade getCidade() {
        return cidade;
    }

    public void setCidade(Cidade cidade) {
        this.cidade = cidade;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getCodigoIata() {
        return codigoIata;
    }

    public void setCodigoIata(String codigoIata) {
        this.codigoIata = codigoIata;
    }
}

