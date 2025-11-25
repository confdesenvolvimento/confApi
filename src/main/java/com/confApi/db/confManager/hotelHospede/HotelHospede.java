package com.confApi.db.confManager.hotelHospede;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author helder.figueiredo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelHospede implements Serializable{
    private Integer codgHotelHospede;
    private String tipoHospede;
    private String hospedeNome;
    private String hospedeSobrenome;
    private Integer hospedeSexo;
    private Integer hospedeIdade;
}
