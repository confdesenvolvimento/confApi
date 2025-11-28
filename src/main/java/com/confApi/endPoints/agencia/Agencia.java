package com.confApi.endPoints.agencia;

import com.confApi.endPoints.unidade.Unidade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agencia {
    private Integer codgAgencia;
    private String nomeAgencia;
    private String cnpj;
    private String codgSistemaBackOffice;
    private Unidade codgUnidade;
    private Integer status;
    private String statusEmissao;
    private String logomarca;
    private String email;
    private String telefone;
    private String endereco;
    private String cidade;
    private String estado;
    private String bairro;
    private String cep;
    private String complemento;
    private Integer idWoobaAgencia;
    private String usuarioApi;
    private String senhaApi;

    public Agencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
        this.nomeAgencia = codgAgencia.toString();
    }

    public Agencia(com.confApi.db.confManager.agencia.dto.Agencia agencia) {
        this.codgAgencia = agencia.getCodgAgencia();
        this.nomeAgencia = agencia.getNomeAgencia();
        this.cnpj = agencia.getCnpj();
        this.codgSistemaBackOffice = agencia.getCodgSistemaBackOffice();
        this.codgUnidade = new Unidade(agencia.getCodgUnidade());
        this.status = agencia.getStatus();
        this.statusEmissao = agencia.getStatusEmissao();
        this.logomarca = agencia.getLogomarca();
        this.email = agencia.getEmail();
        this.telefone = agencia.getTelefone();
        this.endereco = agencia.getEndereco();
        this.cidade = agencia.getCidade();
        this.estado = agencia.getEstado();
        this.bairro = agencia.getBairro();
        this.cep = agencia.getCep();
        this.complemento = agencia.getComplemento();
        this.idWoobaAgencia = agencia.getIdWoobaAgencia();
        this.usuarioApi = agencia.getUsuarioApi();
        this.senhaApi = agencia.getSenhaApi();
    }
}
