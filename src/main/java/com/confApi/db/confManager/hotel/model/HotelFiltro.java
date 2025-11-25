package com.confApi.db.confManager.hotel.model;

import com.confApi.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.confApi.db.confManager.hotel.HotelService.formatarDistancia;


public class HotelFiltro implements Serializable {

    private Boolean is1Estrela = true;
    private Boolean is2Estrela = true;
    private Boolean is3Estrela = true;
    private Boolean is4Estrela = true;
    private Boolean is5Estrela = true;
    private Boolean isFiltrado = false;
    private Integer valorInicial = null;
    private Integer valorFinal = 99999;
    private Integer valorInicialOriginal = 0;
    private Integer valorFinalOriginal = 99999;
    private List<String> regimes = new ArrayList<>();
    private List<String> bairros = new ArrayList<>();
    private List<String> enderecos = new ArrayList<>();

    private List<String> bairrosSelecionados = new ArrayList<>();
    private List<String> nomeHoteis = new ArrayList<>();
    private List<String> regimesSelecionados = new ArrayList<>();
    private List<String> nomeHoteisSelecionados = new ArrayList<>();
    private List<String> enderecosSelecionados = new ArrayList<>();
    private String enderecoSelecionado;
    private String latitudeSugestao;
    private String longitudeSugestao;
    private List<String> sugestoesEnderecos = new ArrayList<>();

    public void filtrarCategoria(List<HotelResponse> original, List<HotelResponse> filtro) {
        //UtilDebug.sysError("Entrou Filtrar " + is2Estrela);
        filtro.clear();
        for (HotelResponse h : original) {

            int categoria = h.getCategoria();
            if ((is1Estrela && categoria == 1)
                    || (is2Estrela && categoria == 2)
                    || (is3Estrela && categoria == 3)
                    || (is4Estrela && categoria == 4)
                    || (is5Estrela && categoria == 5)) {
                filtro.add(h);
            }
        }
        isFiltrado = true;

    }

    public void orderByPontoEnteresse(List<HotelResponse> hotelResponse) {
        if (latitudeSugestao != null && longitudeSugestao != null) {

            for (HotelResponse hResponse : hotelResponse) {
                PontoInteresse pontoInteresse = new PontoInteresse();
                pontoInteresse.setNomePontoInterresse(enderecoSelecionado);
                pontoInteresse.setLatitude(latitudeSugestao);
                pontoInteresse.setLongitude(longitudeSugestao);
                Double distancia = Util.calcularDistancia(hResponse.getLatitude(), hResponse.getLongitude(), Float.valueOf(latitudeSugestao), Float.valueOf(longitudeSugestao));
                pontoInteresse.setValorDistancia(distancia);
                String[] nomePonto = enderecoSelecionado.split(",");
                pontoInteresse.setDescricao("A distância deste hotel até " + nomePonto[0] + " é de : " + formatarDistancia(pontoInteresse.getValorDistancia()) + ".");
                hResponse.setPontoSelecionado(pontoInteresse);
            }
        }

        // Ordena por ponto de interesse
        if (hotelResponse != null) {
            Collections.sort(hotelResponse, Comparator.comparing(HotelResponse -> HotelResponse.getPontoSelecionado().getValorDistancia()));
        }
        isFiltrado = true;

    }

    // Método para buscar latitude e longitude ao selecionar um endereço
    public void buscarCoordenadas() {
        if (enderecoSelecionado == null || enderecoSelecionado.isEmpty()) {
            return;
        }

        try {
            // Codifica a string corretamente para evitar caracteres inválidos na URL
            String enderecoFormatado = URLEncoder.encode(enderecoSelecionado, StandardCharsets.UTF_8.toString());
            String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" + enderecoFormatado;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Garante leitura correta em UTF-8
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            JSONArray jsonArray = new JSONArray(response.toString());
            if (jsonArray.length() > 0) {
                JSONObject json = jsonArray.getJSONObject(0);
                latitudeSugestao = json.getString("lat");
                longitudeSugestao = json.getString("lon");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void filtrarEndereco(List<HotelResponse> original, List<HotelResponse> filtro) {

        filtro.clear();
        for (HotelResponse h : original) {
            String endereco = h.getEndereco();
            for (String bairroSelecionado : enderecosSelecionados) {
                if (endereco.toLowerCase().equalsIgnoreCase(bairroSelecionado.toLowerCase())) {
                   // UtilDebug.sysError("Endereco selecionado: " + bairroSelecionado);
                    filtro.add(h);
                }
            }

        }
        isFiltrado = true;
    }

    public void filtrarBairro(List<HotelResponse> original, List<HotelResponse> filtro) {

        filtro.clear();
        for (HotelResponse h : original) {
            String nomeBairro = h.getBairro();
            for (String bairroSelecionado : bairrosSelecionados) {
                if (nomeBairro.toLowerCase().equalsIgnoreCase(bairroSelecionado.toLowerCase())) {
                   // UtilDebug.sysError("Bairro selecionado: " + bairroSelecionado);
                    filtro.add(h);
                }
            }

        }
        isFiltrado = true;
    }

    public void filtrarNomeHotel(List<HotelResponse> original, List<HotelResponse> filtro) {

        if (!nomeHoteisSelecionados.isEmpty()) {

            filtro.clear();
            for (HotelResponse h : original) {
                String nome = h.getNome();
                for (String bairroSelecionado : nomeHoteisSelecionados) {
                    if (nome.equalsIgnoreCase(bairroSelecionado)) {
                      //  UtilDebug.sysError("Nome Hotel selecionado: " + bairroSelecionado);
                        filtro.add(h);
                    }
                }

            }
            isFiltrado = true;
        }
    }

    public void filtrarPorValor(List<HotelResponse> original, List<HotelResponse> filtro) {
       // UtilDebug.sysError("valores: " + valorInicial + " - " + valorFinal);
        filtro.clear();
        for (HotelResponse h : original) {
            int tarifaHotel = (int) Math.floor(h.getTarifasHotel().getValorTotalEstadiaComMarkupBrl());

            if (tarifaHotel >= valorInicial && tarifaHotel <= valorFinal) {
                filtro.add(h);
            }

        }
        isFiltrado = true;
    }

    public void filtrarRegime(List<HotelResponse> original, List<HotelResponse> filtro) {
        filtro.clear();

        // Usando Set para busca mais eficiente
        Set<String> regimesSelecionadosSet = new HashSet<>(regimesSelecionados);

        for (HotelResponse h : original) {
            boolean adicionado = false; // Controle para adicionar o hotel apenas uma vez
            for (QuartoPesquisa quartoPesquisa : h.getQuartoPesquisa()) {

                for (HotelAcomodacao ac : quartoPesquisa.getAcomodacoes()) {
                    String regime = ac.getRegime();
                    if (regime != null) {

                        if (regimesSelecionadosSet.contains(regime)) {
                            filtro.add(h);
                            adicionado = true;  // Adiciona o hotel e marca como já adicionado
                            break; // Sai do loop de acomodações assim que encontrar um regime compatível
                        }
                    }
                }
            }
            if (adicionado) {
                continue;
            }
        }

        isFiltrado = true;
    }

    public List<String> getNomeHoteisSelecionados() {
        return nomeHoteisSelecionados;
    }

    public void setNomeHoteisSelecionados(List<String> nomeHoteisSelecionados) {
        this.nomeHoteisSelecionados = nomeHoteisSelecionados;
    }

    public List<String> getBairrosSelecionados() {
        return bairrosSelecionados;
    }

    public void setBairrosSelecionados(List<String> bairrosSelecionados) {
        this.bairrosSelecionados = bairrosSelecionados;
    }

    public Boolean getIs1Estrela() {
        return is1Estrela;
    }

    public void setIs1Estrela(Boolean is1Estrela) {
        this.is1Estrela = is1Estrela;
    }

    public Boolean getIs2Estrela() {
        return is2Estrela;
    }

    public void setIs2Estrela(Boolean is2Estrela) {
        this.is2Estrela = is2Estrela;
    }

    public Boolean getIs3Estrela() {
        return is3Estrela;
    }

    public void setIs3Estrela(Boolean is3Estrela) {
        this.is3Estrela = is3Estrela;
    }

    public Boolean getIs4Estrela() {
        return is4Estrela;
    }

    public void setIs4Estrela(Boolean is4Estrela) {
        this.is4Estrela = is4Estrela;
    }

    public Boolean getIs5Estrela() {
        return is5Estrela;
    }

    public void setIs5Estrela(Boolean is5Estrela) {
        this.is5Estrela = is5Estrela;
    }

    public Integer getValorInicial() {
        return valorInicial;
    }

    public void setValorInicial(Integer valorInicial) {
        this.valorInicial = valorInicial;
    }

    public Integer getValorFinal() {
        return valorFinal;
    }

    public void setValorFinal(Integer valorFinal) {
        this.valorFinal = valorFinal;
    }

    public List<String> getRegimes() {
        return regimes;
    }

    public void setRegimes(List<String> regimes) {
        this.regimes = regimes;
    }

    public List<String> getRegimesSelecionados() {
        return regimesSelecionados;
    }

    public void setRegimesSelecionados(List<String> regimesSelecionados) {
        this.regimesSelecionados = regimesSelecionados;
    }

    public List<String> getBairros() {
        return bairros;
    }

    public void setBairros(List<String> bairros) {
        this.bairros = bairros;
    }

    public List<String> getNomeHoteis() {
        return nomeHoteis;
    }

    public void setNomeHoteis(List<String> nomeHoteis) {
        this.nomeHoteis = nomeHoteis;
    }

    public Integer getValorFinalOriginal() {
        return valorFinalOriginal;
    }

    public void setValorFinalOriginal(Integer valorFinalOriginal) {
        this.valorFinalOriginal = valorFinalOriginal;
    }

    public Integer getValorInicialOriginal() {
        return valorInicialOriginal;
    }

    public void setValorInicialOriginal(Integer valorInicialOriginal) {
        this.valorInicialOriginal = valorInicialOriginal;
    }

    public Boolean getIsFiltrado() {
        return isFiltrado;
    }

    public void setIsFiltrado(Boolean isFiltrado) {
        this.isFiltrado = isFiltrado;
    }

    public List<String> getEnderecosSelecionados() {
        return enderecosSelecionados;
    }

    public void setEnderecosSelecionados(List<String> enderecosSelecionados) {
        this.enderecosSelecionados = enderecosSelecionados;
    }

    public List<String> getEnderecos() {
        return enderecos;
    }

    public void setEnderecos(List<String> enderecos) {
        this.enderecos = enderecos;
    }

    public String getEnderecoSelecionado() {
        return enderecoSelecionado;
    }

    public void setEnderecoSelecionado(String enderecoSelecionado) {
        this.enderecoSelecionado = enderecoSelecionado;
    }

    public String getLatitudeSugestao() {
        return latitudeSugestao;
    }

    public void setLatitudeSugestao(String latitudeSugestao) {
        this.latitudeSugestao = latitudeSugestao;
    }

    public String getLongitudeSugestao() {
        return longitudeSugestao;
    }

    public void setLongitudeSugestao(String longitudeSugestao) {
        this.longitudeSugestao = longitudeSugestao;
    }

    public List<String> getSugestoesEnderecos() {
        return sugestoesEnderecos;
    }

    public void setSugestoesEnderecos(List<String> sugestoesEnderecos) {
        this.sugestoesEnderecos = sugestoesEnderecos;
    }

}

