package com.confApi.db.clube.banner;


import com.confApi.db.clube.arquivoAnexo.ArquivoAnexo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Banner implements Serializable {

    private Integer codgBanner;
    private String titulo;
    private String descricao;
    private ArquivoAnexo arquivo;
    private int status;
    private Date dataCadastro;
}
