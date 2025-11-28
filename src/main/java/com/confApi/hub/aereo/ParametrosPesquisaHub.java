package com.confApi.hub.aereo;

import com.confApi.hub.enumerador.ClasseHub;
import com.confApi.hub.enumerador.OrdenacaoHub;
import com.confApi.hub.enumerador.TipoBagagemHub;
import com.confApi.hub.enumerador.TipoTarifaHub;
import lombok.Data;

@Data
public class ParametrosPesquisaHub {
    private ClasseHub classe;
    private OrdenacaoHub ordenacao;
    private TipoBagagemHub tipoBagagem;
    private TipoTarifaHub tarifa;
}
