package com.confApi.db.confManager.agencia;

import com.confApi.db.confManager.unidade.Unidade;
import com.confApi.db.confManager.unidade.UnidadeService;
import com.confApi.db.sica.empresa.Empresa;
import com.confApi.db.wooba.agencia.TurAgencia;
import com.confApi.db.wooba.unidade.TurUnidadesOperacionais;
import com.confApi.db.wooba.unidade.TurUnidadesOperacionaisService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public Agencia() {
    }

    public Integer getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public String getNomeAgencia() {
        return nomeAgencia;
    }

    public void setNomeAgencia(String nomeAgencia) {
        this.nomeAgencia = nomeAgencia;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCodgSistemaBackOffice() {
        return codgSistemaBackOffice;
    }

    public void setCodgSistemaBackOffice(String codgSistemaBackOffice) {
        this.codgSistemaBackOffice = codgSistemaBackOffice;
    }

    public Unidade getCodgUnidade() {
        return codgUnidade;
    }

    public void setCodgUnidade(Unidade codgUnidade) {
        this.codgUnidade = codgUnidade;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusEmissao() {
        return statusEmissao;
    }

    public void setStatusEmissao(String statusEmissao) {
        this.statusEmissao = statusEmissao;
    }

    public String getLogomarca() {
        return logomarca;
    }

    public void setLogomarca(String logomarca) {
        this.logomarca = logomarca;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;

    }

    public Integer getIdWoobaAgencia() {
        return idWoobaAgencia;
    }

    public void setIdWoobaAgencia(Integer idWoobaAgencia) {
        this.idWoobaAgencia = idWoobaAgencia;
    }

    public String getUsuarioApi() {
        return usuarioApi;
    }

    public void setUsuarioApi(String usuarioApi) {
        this.usuarioApi = usuarioApi;
    }

    public String getSenhaApi() {
        return senhaApi;
    }

    public void setSenhaApi(String senhaApi) {
        this.senhaApi = senhaApi;
    }
}
