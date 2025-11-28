package com.confApi.hub.aereo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BilheteHub {
    private Boolean bilheteDoInfantil;
    private Date dataDeEmissao;
    private String numero;
    private List<PagamentoHub> pagamentos;
    @JsonIgnore
    private String passageiro;
    @JsonIgnore
    private List<PassageiroHub> passageirosAdicionais;
    private String paxRef;
    private String status;
    private List<VooHub> voos;
}
