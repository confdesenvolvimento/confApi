package com.confApi.db.confManager.seguro.reserva.DTO;

import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.seguros.dto.SeguroCompraModel;
import lombok.Data;

@Data
public class CancelamentoRequestDTO {

    private Integer codgReserva;
    private Integer codgUsuarioCancelamento;
    private String descricaoMotivoCancelamento;
    private String localizador;
    private String operacao;
    private String cpfoudocumento;

    public CancelamentoRequestDTO(SeguroReserva seguroReserva) {
        this.codgReserva = seguroReserva.getCodgReservaSeguro();
        this.codgUsuarioCancelamento = seguroReserva.getCodgUsuarioCriacao().getCodgUsuario();
        this.descricaoMotivoCancelamento = "Não foi possível realizar a compra.";
        this.localizador = seguroReserva.getLocalizador();
        this.operacao = null;
        this.cpfoudocumento = seguroReserva.getSeguradosList().get(0).getCpf();
    }

    public CancelamentoRequestDTO() {
    }

    public CancelamentoRequestDTO(Integer codgReserva, Integer codgUsuarioCancelamento, String descricaoMotivoCancelamento, String localizador, String operacao, String cpfoudocumento) {
        this.codgReserva = codgReserva;
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.localizador = localizador;
        this.operacao = operacao;
        this.cpfoudocumento = cpfoudocumento;
    }
}
