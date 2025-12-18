package com.confApi.db.confManager.usuario.dto.autenticar;

import lombok.Data;

@Data
public class AgenciaDTO {
    private Long id;
    private String nome;
    private String idErp;
    private String cnpj;
    private Integer status;
    private Integer statusEmissao;
    private String logomarca;
    private String razaoSocial;

    public AgenciaDTO() {}

    public AgenciaDTO(Long id, String nome, String idErp, String cnpj, Integer status,
                      Integer statusEmissao, String logomarca, String razaoSocial) {
        this.id = id;
        this.nome = nome;
        this.idErp = idErp;
        this.cnpj = cnpj;
        this.status = status;
        this.statusEmissao = statusEmissao;
        this.logomarca = logomarca;
        this.razaoSocial = razaoSocial;
    }

    // getters/setters
}