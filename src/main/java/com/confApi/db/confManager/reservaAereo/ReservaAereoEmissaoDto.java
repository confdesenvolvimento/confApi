package com.confApi.db.confManager.reservaAereo;

import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.Data;

import java.util.Date;

@Data
public class ReservaAereoEmissaoDto {
    private Long codgReservaAereo;
    private Date dataEmissao;
    private Long codgUsuarioEmissao;

    public ReservaAereoEmissaoDto() {
    }

    public ReservaAereoEmissaoDto(Long codgReservaAereo, Date dataEmissao, Long codgUsuarioEmissao) {
        this.codgReservaAereo = codgReservaAereo;
        this.dataEmissao = dataEmissao;
        this.codgUsuarioEmissao = codgUsuarioEmissao;
    }

    public ReservaAereoEmissaoDto(ReservaAereoModel reservaAerea, Usuario usuario) {
        this.codgReservaAereo = reservaAerea.getCodgReservaAereoDB();
        this.dataEmissao = new Date();
        this.codgUsuarioEmissao = Long.valueOf(usuario.getCodgUsuario());
    }

    public Long getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(Long codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Long getCodgUsuarioEmissao() {
        return codgUsuarioEmissao;
    }

    public void setCodgUsuarioEmissao(Long codgUsuarioEmissao) {
        this.codgUsuarioEmissao = codgUsuarioEmissao;
    }
}
