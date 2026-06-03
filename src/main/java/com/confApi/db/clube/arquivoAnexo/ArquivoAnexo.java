package com.confApi.db.clube.arquivoAnexo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArquivoAnexo implements Serializable {

    private Integer codgArquivoAnexo;
    private String titulo;
    private String nome;
    private String nomeFTP;
    private String type;
    private String pasta;
    private String extencao;
    private Integer size;

}
