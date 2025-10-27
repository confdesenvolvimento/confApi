package com.confApi.db.confManager.faturas.dto.model;

import lombok.Data;

@Data
public class NotaFiscal {
    private String cia;
    private double valor;
    private String nome;
    private String endereco;
    private String cnpj;
    private String ie;
    private String cidade;
    private String uf;
    private int fornecedor;
    private double valorInternacional;
    private double valorNacional;
    private double issrf;
}
