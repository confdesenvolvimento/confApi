package com.confApi.seguros.dto;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
@Data
public class SeguroReservaDTO implements Serializable {
    private Integer codgReservaSeguro;
    private String localizador;

    private Integer status;
    private Integer statusPagamentoCliente;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private LocalDateTime prazoEmissaoCliente;
    private LocalDateTime dataCancelamento;

    private Date dataInicioCobertura;
    private Date dataFinalCobertura;

    private Double valorTotalReservaNet;
    private Double valorTotalReservaMarkup;
    private Double valorTotal; // opcional (net + markup)

    // Identificação
    private Integer codgAgencia;
    private String nomeAgencia;
    private Integer codgUsuario;
    private String loginUsuario;
    private String nomeUsuario;
    private Integer codgErp;
    private Integer codgUnidade;

    // Cobertura (1)
    private PlanoSeguroDTO  cobertura;
    //private List<CoberturaSeguroDTO> detalhesCoberturaList;
    // Segurados (N)
    private List<SeguradoDTO> segurados;

    // Contato emergência (opcional)
    private ContatoEmergenciaDTO contatoEmergencia ;
    private String motivoCancelamento;
    // Recebimento/pagamento (opcional)
   // private SeguroRecebimentoDTO recebimento;
}
