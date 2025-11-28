package com.confApi.endPoints.formaPagamento;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.endPoints.bandeira.BandeiraResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormaPagamentoResponse {
    private List<BandeiraResponse> bandeiras;

    public FormaPagamentoResponse(FormaPagamento formaPagamento) {
        this.bandeiras = new ArrayList<>();
        this.bandeiras.add(new BandeiraResponse(formaPagamento));
    }
}
