package com.confApi.db.confManager.agencia.dto;

import com.confApi.db.confManager.unidade.dto.Unidade;
import com.confApi.db.confManager.unidade.UnidadeService;
import com.confApi.db.sica.empresa.Empresa;
import com.confApi.db.wooba.agencia.TurAgencia;
import com.confApi.db.wooba.unidade.TurUnidadesOperacionais;
import com.confApi.db.wooba.unidade.TurUnidadesOperacionaisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class Agencia implements Serializable {

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

    private Unidade unidade = new Unidade();

    public Agencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public Agencia(Empresa empresa) {
        this.nomeAgencia = empresa.getNome();
        this.cnpj = empresa.getCnpj();
        this.codgSistemaBackOffice = String.valueOf(empresa.getCodemp());
        this.codgUnidade = new Unidade(empresa.getCodUnidade(), null);
        this.status = 1;
        this.email = empresa.getEmail();
        if (empresa.getFone2() != null) {
            this.telefone = empresa.getFone1() + " ou " + empresa.getFone2();
        } else {
            this.telefone = empresa.getFone1();
        }
        this.statusEmissao = "Ambas";
        this.endereco = empresa.getEndereco();
        this.cidade = empresa.getCidade();
        this.estado = empresa.getUF();
        this.bairro = empresa.getBairro();
        this.cep = empresa.getCep();

    }

    public Agencia(TurAgencia turAgencia) {
        this.nomeAgencia = turAgencia.getNome();
        this.cnpj = turAgencia.getCnpj();
        this.codgSistemaBackOffice = turAgencia.getIdErp();

        Boolean unidadeExiste = new UnidadeService().findAllParamsExite(new Unidade(null, turAgencia.getTurUnidadesOperacionais()));
        if (unidadeExiste) {
            Unidade uni = new UnidadeService().findAllParams(new Unidade(null, turAgencia.getTurUnidadesOperacionais()));

            this.codgUnidade = new Unidade(uni.getCodgUnidade());
        } else {

            try {
                TurUnidadesOperacionais turUnidadesOperacionais = new TurUnidadesOperacionaisService().findByCodgUnidade(turAgencia.getTurUnidadesOperacionais());
                Unidade u = new Unidade(turUnidadesOperacionais);
                unidade = new UnidadeService().create(u);
                this.codgUnidade = new Unidade(unidade.getCodgUnidade());
            } catch (JsonProcessingException ex) {
                Logger.getLogger(Agencia.class.getName()).log(Level.SEVERE, null, ex);

            }
        }
        this.status = turAgencia.getStatus();
        this.email = turAgencia.getEmail();
        this.statusEmissao = "Ambas";
        this.logomarca = "https://cdn.portaldoagente.com.br/Logomarcas/" + turAgencia.getLogomarca();
        this.telefone = turAgencia.getTelefone();
        this.endereco = turAgencia.getEndereco();
        this.cidade = turAgencia.getCidade();
        this.estado = turAgencia.getEstado();
        this.bairro = turAgencia.getBairro();
        this.cep = turAgencia.getCep();
        this.complemento = turAgencia.getComplemento();
        this.idWoobaAgencia = turAgencia.getId();
    }

    public Agencia(AgenciaClienteDto agenciaDto) {
        this.codgAgencia = agenciaDto.getCodgAgencia();
    }

    public Agencia(com.confApi.endPoints.agencia.Agencia agencia) {
        this.codgAgencia = agencia.getCodgAgencia();
        this.nomeAgencia = agencia.getNomeAgencia();
        this.cnpj = agencia.getCnpj();
        this.codgSistemaBackOffice = agencia.getCodgSistemaBackOffice();
        this.codgUnidade = agencia.getCodgUnidade() != null ? new Unidade(agencia.getCodgUnidade()) : null;
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
        this.unidade = agencia.getCodgUnidade() != null ? new Unidade(agencia.getCodgUnidade()) : null;
    }

    public Agencia() {
    }

}
