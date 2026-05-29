package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Pagamento;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.Voo;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class Bilhete {
    private Boolean bilheteDoInfantil;
    private Date dataDeEmissao;
    private String numero;
    private List<Pagamento> pagamentos;
    private String passageiro;
    private List<Passageiro> passageirosAdicionais;
    private String paxRef;
    private String status;
    private List<Voo> voos;
}
