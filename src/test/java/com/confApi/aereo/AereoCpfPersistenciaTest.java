package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.DocumentoPassageiro;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AereoCpfPersistenciaTest {
    private Passageiro retorno(String cpf, Integer tipoDocumento, String numero) {
        Passageiro p = new Passageiro(); p.setNome("TESTE"); p.setSobrenome("FICTICIO"); p.setFaixaEtaria("ADT"); p.setCpf(cpf);
        if (tipoDocumento != null) {
            DocumentoPassageiro doc = new DocumentoPassageiro(); doc.setTipo(tipoDocumento); doc.setNumero(numero); p.setDocumento(doc);
        }
        return p;
    }
    private PassageiroModel original(String cpf) {
        PassageiroModel p = new PassageiroModel(); p.setNome("TESTE"); p.setSobrenome("FICTICIO"); p.setFaixaEtaria("ADT"); p.setCpf(cpf); return p;
    }
    private com.confApi.db.confManager.passageiro.Passageiro converter(Passageiro p, PassageiroModel... originais) {
        Reserva r = mock(Reserva.class, RETURNS_DEEP_STUBS);
        TrechoReserva trecho = mock(TrechoReserva.class, RETURNS_DEEP_STUBS);
        when(trecho.getVoos()).thenReturn(List.of()); when(r.getViagens()).thenReturn(List.of(trecho));
        when(r.getPrazoEmissao()).thenReturn("2026-10-22T12:00:00.000-03:00");
        when(r.getPassageiros()).thenReturn(List.of(p)); when(r.getContatos()).thenReturn(List.of(new Contato()));
        when(r.getValorReserva().getValorBase().getValorPassageiroList()).thenReturn(List.of());
        PreReserva pre = new PreReserva(); UsuarioDto user = new UsuarioDto(); user.setId(1); pre.setUsuario(user); pre.setPassageiros(List.of(originais));
        return new AereoService().convertToReservaAereo(r, pre).getPassageiros().get(0);
    }
    @Test void recuperaCpfDoDocumentoQuandoFornecedorRetornaCampoCpfNulo() {
        Passageiro resposta = retorno(null, 1, "00123456789");
        var db = converter(resposta);
        assertEquals("00123456789", db.getCpf()); assertEquals("00123456789", db.getNumrDocumento());
        assertNull(resposta.getCpf(), "A correção é local à persistência; não muda o retorno do fornecedor");
    }
    @Test void normalizaCpfComMascaraPreservandoZeros() {
        assertEquals("00123456789", converter(retorno(" 001.234.567-89 ", null, null)).getCpf());
    }
    @Test void preservaCpfRecebidoSemSubstituirPorOutroDaPreReserva() {
        assertEquals("00123456789", converter(retorno("00123456789", null, null), original("99999999999")).getCpf());
    }
    @Test void recuperaCpfDaPreReservaQuandoFornecedorOmiteDocumentos() {
        assertEquals("00123456789", converter(retorno(null, null, null), original("001.234.567-89")).getCpf());
    }
    @Test void recuperaCpfDoDocumentoDaPreReserva() {
        PassageiroModel p = original(null); p.getDocumento().setTipo(1); p.getDocumento().setNumero("00123456789");
        assertEquals("00123456789", converter(retorno(" ", null, null), p).getCpf());
    }
    @Test void naoTransformaPassaporteNumericoEmCpf() {
        assertNull(converter(retorno(null, 2, "00123456789")).getCpf());
    }
    @Test void documentoCpfIncompletoNaoEhUsadoComoCpf() {
        assertNull(converter(retorno(null, 1, "12345")).getCpf());
    }
    @Test void naoUsaCpfDeOutroPassageiro() {
        PassageiroModel outro = original("00123456789"); outro.setNome("OUTRO");
        assertNull(converter(retorno(null, null, null), outro).getCpf());
    }
    @Test void homonimosComCpfsDiferentesNaoSaoAssociadosArbitrariamente() {
        assertNull(converter(retorno(null, null, null), original("00123456789"), original("99999999999")).getCpf());
    }
    @Test void naoUsaCpfDoAdultoParaBebeHomonimo() {
        Passageiro bebe = retorno(null, null, null); bebe.setFaixaEtaria("INF");
        PassageiroModel originalBebe = original("00123456789"); originalBebe.setFaixaEtaria("INF");
        assertEquals("00123456789", converter(bebe, original("99999999999"), originalBebe).getCpf());
    }
}
