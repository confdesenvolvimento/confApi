package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeConfiguracaoMassaRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeConfiguracaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeVinculosRequest;
import com.confApi.chatconfianca.dto.response.DepartamentoUnidadeConfiguracaoMassaResponse;
import com.confApi.exception.RegraDeNegocioException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatConfiancaConfigServiceTest {

    @Mock
    private ChatConfiancaManagerClient manager;

    @InjectMocks
    private ChatConfiancaConfigService service;

    @Test
    void consultaUsuariosReferenciaPeloEndpointDeConsulta() {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUsuario(6000);
        when(manager.getList(
                eq("chat-confianca/consultas/usuarios"),
                any())).thenReturn(List.of(usuario));
        when(manager.get(
                "chat-confianca/consultas/usuarios/6000",
                RefUsuario.class)).thenReturn(usuario);

        List<RefUsuario> usuarios = service.listarUsuariosReferencia();
        RefUsuario encontrado = service.buscarUsuarioReferencia(6000);

        assertEquals(List.of(usuario), usuarios);
        assertEquals(usuario, encontrado);
        verify(manager).getList(
                eq("chat-confianca/consultas/usuarios"),
                any(ParameterizedTypeReference.class));
        verify(manager).get(
                "chat-confianca/consultas/usuarios/6000",
                RefUsuario.class);
    }

    @Test
    void normalizaVinculosEEncaminhaParaPersistencia() {
        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setAtivo(true);
        when(manager.get(
                "chat-confianca/persistencia/departamentos/4",
                Departamento.class)).thenReturn(departamento);
        when(manager.getList(
                eq("chat-confianca/persistencia/departamentos"),
                any())).thenReturn(List.of(departamento));

        RefUnidade unidadeUm = new RefUnidade();
        unidadeUm.setCodgUnidade(1);
        RefUnidade unidadeDois = new RefUnidade();
        unidadeDois.setCodgUnidade(2);
        when(manager.getList(
                eq("chat-confianca/persistencia/unidades"),
                any())).thenReturn(List.of(unidadeUm, unidadeDois));
        when(manager.getList(eq("unidade"), any())).thenReturn(List.of());

        DepartamentoUnidadeVinculosRequest request =
                new DepartamentoUnidadeVinculosRequest();
        request.setDepartamentoId(4L);
        request.setCodigosUnidade(Arrays.asList(2, null, 1, 2));

        DepartamentoUnidade atualizado = new DepartamentoUnidade();
        atualizado.setId(11L);
        when(manager.postList(
                eq("chat-confianca/persistencia/departamento-unidades/vinculos"),
                eq(request),
                any())).thenReturn(List.of(atualizado));

        List<DepartamentoUnidade> resultado =
                service.salvarVinculosDepartamentoUnidades(request);

        assertEquals(List.of(atualizado), resultado);
        assertEquals(List.of(2, 1), request.getCodigosUnidade());
        verify(manager).postList(
                eq("chat-confianca/persistencia/departamento-unidades/vinculos"),
                eq(request),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void bloqueiaDepartamentoAgrupadorAntesDeEncaminharVinculos() {
        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setAtivo(true);
        Departamento filho = new Departamento();
        filho.setId(5L);
        filho.setDepartamentoPaiId(4L);
        when(manager.get(
                "chat-confianca/persistencia/departamentos/4",
                Departamento.class)).thenReturn(departamento);
        when(manager.getList(
                eq("chat-confianca/persistencia/departamentos"),
                any())).thenReturn(List.of(departamento, filho));

        DepartamentoUnidadeVinculosRequest request =
                new DepartamentoUnidadeVinculosRequest();
        request.setDepartamentoId(4L);
        request.setCodigosUnidade(List.of(1));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarVinculosDepartamentoUnidades(request));

        assertEquals(400, erro.getStatus());
        verify(manager, never()).postList(
                eq("chat-confianca/persistencia/departamento-unidades/vinculos"),
                any(),
                any());
    }

    @Test
    void encaminhaConfiguracaoIndividualAoEndpointDedicado() {
        DepartamentoUnidadeConfiguracaoRequest request =
                new DepartamentoUnidadeConfiguracaoRequest();
        request.setMensagemAbertura("Ola, como podemos ajudar?");
        DepartamentoUnidade atualizado = new DepartamentoUnidade();
        atualizado.setId(11L);
        atualizado.setMensagemAbertura(request.getMensagemAbertura());
        when(manager.post(
                "chat-confianca/persistencia/departamento-unidades/11/configuracao",
                request,
                DepartamentoUnidade.class)).thenReturn(atualizado);

        DepartamentoUnidade resultado =
                service.salvarConfiguracaoDepartamentoUnidade(11L, request);

        assertEquals(atualizado, resultado);
        verify(manager).post(
                "chat-confianca/persistencia/departamento-unidades/11/configuracao",
                request,
                DepartamentoUnidade.class);
    }

    @Test
    void validaIdERequestAntesDeEncaminharConfiguracaoIndividual() {
        DepartamentoUnidadeConfiguracaoRequest request =
                new DepartamentoUnidadeConfiguracaoRequest();

        RegraDeNegocioException erroId = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarConfiguracaoDepartamentoUnidade(null, request));
        RegraDeNegocioException erroRequest = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarConfiguracaoDepartamentoUnidade(11L, null));

        assertEquals(400, erroId.getStatus());
        assertEquals(400, erroRequest.getStatus());
        verify(manager, never()).post(any(), any(), any());
    }

    @Test
    void encaminhaConfiguracaoMassaAoEndpointDedicado() {
        DepartamentoUnidadeConfiguracaoMassaRequest request =
                new DepartamentoUnidadeConfiguracaoMassaRequest();
        request.setAlterarMensagemAbertura(true);
        request.setMensagemAbertura("Ola, como podemos ajudar?");
        DepartamentoUnidadeConfiguracaoMassaResponse resposta =
                new DepartamentoUnidadeConfiguracaoMassaResponse();
        resposta.setTotalUnidadesAtualizadas(57);
        when(manager.post(
                "chat-confianca/persistencia/departamentos/4/unidades/configuracao",
                request,
                DepartamentoUnidadeConfiguracaoMassaResponse.class)).thenReturn(resposta);

        DepartamentoUnidadeConfiguracaoMassaResponse resultado =
                service.salvarConfiguracaoMassaDepartamentoUnidades(4L, request);

        assertEquals(57, resultado.getTotalUnidadesAtualizadas());
        verify(manager).post(
                "chat-confianca/persistencia/departamentos/4/unidades/configuracao",
                request,
                DepartamentoUnidadeConfiguracaoMassaResponse.class);
    }

    @Test
    void exigeDepartamentoRequestECampoNaConfiguracaoMassa() {
        DepartamentoUnidadeConfiguracaoMassaRequest semCampos =
                new DepartamentoUnidadeConfiguracaoMassaRequest();
        DepartamentoUnidadeConfiguracaoMassaRequest comCampo =
                new DepartamentoUnidadeConfiguracaoMassaRequest();
        comCampo.setAlterarExigeAssunto(true);
        comCampo.setExigeAssunto(true);

        RegraDeNegocioException erroId = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarConfiguracaoMassaDepartamentoUnidades(null, comCampo));
        RegraDeNegocioException erroRequest = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarConfiguracaoMassaDepartamentoUnidades(4L, null));
        RegraDeNegocioException erroCampos = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarConfiguracaoMassaDepartamentoUnidades(4L, semCampos));

        assertEquals(400, erroId.getStatus());
        assertEquals(400, erroRequest.getStatus());
        assertEquals(400, erroCampos.getStatus());
        verify(manager, never()).post(any(), any(), any());
    }
}
