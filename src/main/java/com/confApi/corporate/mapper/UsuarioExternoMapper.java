package com.confApi.corporate.mapper;

import com.confApi.corporate.dto.usuarioExternoDTO.AgenciaExternoDTO;
import com.confApi.corporate.dto.usuarioExternoDTO.UnidadeExternoDTO;
import com.confApi.corporate.dto.usuarioExternoDTO.UsuarioExternoDTO;
import com.confApi.corporate.dto.usuarioExternoDTO.UsuarioExternoResponseDTO;

import java.util.Map;

public class UsuarioExternoMapper {

    @SuppressWarnings("unchecked")
    public static UsuarioExternoResponseDTO toDTO(Map<String, Object> origem) {

        UsuarioExternoResponseDTO response = new UsuarioExternoResponseDTO();

        // -------- USUÁRIO --------
        Map<String, Object> u = (Map<String, Object>) origem.get("usuario");
        if (u != null) {
            UsuarioExternoDTO usuario = new UsuarioExternoDTO();
            usuario.setLogin((String) u.get("nome"));
            usuario.setEmail((String) u.get("email"));
            usuario.setTelefone((String) u.get("telefone"));
            usuario.setCelular((String) u.get("celular"));
            usuario.setCpf((String) u.get("cpf"));
            usuario.setNomeCompleto((String) u.get("nomeCompleto"));
            usuario.setPrimeiroNome((String) u.get("primeiroNome"));
            usuario.setUltimoNome((String) u.get("ultimoNome"));
            usuario.setSexo((String) u.get("sexo"));
            usuario.setToken((String) u.get("tokenIdentificacao"));
            usuario.setPodeEmitir(Boolean.TRUE.equals(u.get("podeEmitir")));
            usuario.setStatus(parseInt(u.get("status")));

            response.setUsuario(usuario);
        }

        // -------- AGÊNCIA --------
        Map<String, Object> a = (Map<String, Object>) origem.get("agencia");
        if (a != null) {
            AgenciaExternoDTO agencia = new AgenciaExternoDTO();
            agencia.setId(parseInt(a.get("id")));
            agencia.setNome(trim((String) a.get("nome")));
            agencia.setCodigoErp((String) a.get("idErp"));
            agencia.setCnpj((String) a.get("cnpj"));
            agencia.setStatus(parseInt(a.get("status")));
            agencia.setStatusEmissao(parseInt(a.get("statusEmissao")));
            agencia.setRazaoSocial((String) a.get("razaoSocial"));
            agencia.setLogomarca((String) "https://cdn.portaldoagente.com.br/Logomarcas/"+a.get("logomarca"));

            response.setAgencia(agencia);
        }

        // -------- UNIDADE --------
        Map<String, Object> un = (Map<String, Object>) origem.get("unidade");
        if (un != null) {
            UnidadeExternoDTO unidade = new UnidadeExternoDTO();
            unidade.setId(parseInt(un.get("id")));
            unidade.setNome((String) un.get("nome"));
            response.setUnidade(unidade);
        }

        response.setMensagem((String) origem.get("mensagem"));
        return response;
    }

    // -------- Helpers --------

    private static Integer parseInt(Object o) {
        if (o == null) return null;
        return Integer.valueOf(String.valueOf(o));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
