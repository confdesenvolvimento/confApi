package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.enums.DistribuicaoDepartamento;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuracao que pode ser aplicada a todos os vinculos ativos de um
 * departamento. Cada flag determina se o valor correspondente deve ser
 * alterado; campos sem flag permanecem inalterados em cada unidade.
 */
@Data
@NoArgsConstructor
public class DepartamentoUnidadeConfiguracaoMassaRequest {

    private Boolean alterarNomeExibicao;
    private Boolean alterarPermiteChamadoAgencia;
    private Boolean alterarPermiteChamadoInterno;
    private Boolean alterarRecebeRemarcacaoAerea;
    private Boolean alterarExigeAssunto;
    private Boolean alterarDistribuicao;
    private Boolean alterarLimiteChatsPorAtendente;
    private Boolean alterarMensagemAbertura;
    private Boolean alterarMensagemForaHorario;

    private String nomeExibicao;
    private Boolean permiteChamadoAgencia;
    private Boolean permiteChamadoInterno;
    private Boolean recebeRemarcacaoAerea;
    private Boolean exigeAssunto;
    private DistribuicaoDepartamento distribuicao;
    private Integer limiteChatsPorAtendente;
    private String mensagemAbertura;
    private String mensagemForaHorario;

    public boolean possuiCampoParaAlterar() {
        return Boolean.TRUE.equals(alterarNomeExibicao)
                || Boolean.TRUE.equals(alterarPermiteChamadoAgencia)
                || Boolean.TRUE.equals(alterarPermiteChamadoInterno)
                || Boolean.TRUE.equals(alterarRecebeRemarcacaoAerea)
                || Boolean.TRUE.equals(alterarExigeAssunto)
                || Boolean.TRUE.equals(alterarDistribuicao)
                || Boolean.TRUE.equals(alterarLimiteChatsPorAtendente)
                || Boolean.TRUE.equals(alterarMensagemAbertura)
                || Boolean.TRUE.equals(alterarMensagemForaHorario);
    }
}
