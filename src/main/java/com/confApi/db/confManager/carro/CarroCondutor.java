package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.CarroBookingCondutorHub;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
public class CarroCondutor {

    private CarroReserva carroReserva;
    private String nome;
    private String sobrenome;
    private String numrDocumento;
    private String tipoDocumento;
    private String email;
    private Date dataNascimento;
    private String numrRelefone;
    private String numrCodgAreaTelefone;
    private String numrCodgPaisTelefone;
    private String nacionalidade;
    private String airlineCode;
    private String numeroFlight;

    public CarroCondutor(CarroBookingCondutorHub obj) {
        this(obj, null);
    }

    public CarroCondutor(CarroBookingCondutorHub obj, CarroReserva carroReserva) {
        this.carroReserva = carroReserva;
        this.nome = obj != null ? obj.getNome() : null;
        this.sobrenome = obj != null ? obj.getSobrenome() : null;
        this.numrDocumento = obj != null ? obj.getDocumento() : null;
        this.tipoDocumento = obj != null ? obj.getTipoDocumento() : null;
        this.email = null;
        this.dataNascimento = obj != null ? parseDataNascimento(obj.getNascimento()) : null;
        this.numrRelefone = obj != null ? obj.getTelefone() : null;
        this.numrCodgAreaTelefone = obj != null ? obj.getCodigoArea() : null;
        this.numrCodgPaisTelefone = obj != null ? obj.getCodigoPais() : null;
        this.nacionalidade = null;
        this.airlineCode = null;
        this.numeroFlight = null;
    }

    private static Date parseDataNascimento(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String[] patterns = {
                "dd/MM/yyyy",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false);
                return formatter.parse(value.trim());
            } catch (ParseException ignored) {
            }
        }

        throw new RuntimeException("Data de nascimento inválida: " + value);
    }
}
