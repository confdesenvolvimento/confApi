package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.enums.DistribuicaoDepartamento;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Campos configuraveis de um vinculo individual entre departamento e unidade.
 *
 * <p>O identificador e a associacao departamento/unidade nao fazem parte deste
 * contrato para que uma edicao de configuracao nao altere o vinculo.</p>
 */
@Data
@NoArgsConstructor
public class DepartamentoUnidadeConfiguracaoRequest {

    private String nomeExibicao;
    private Boolean permiteChamadoAgencia;
    private Boolean permiteChamadoInterno;
    private Boolean exigeAssunto;
    private DistribuicaoDepartamento distribuicao;
    private Integer limiteChatsPorAtendente;
    private String mensagemAbertura;
    private String mensagemForaHorario;
}
