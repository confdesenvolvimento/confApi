package com.confApi.db.clube.campanha;

import com.confApi.db.clube.campanha.dto.CampanhaRankingDTO;
import com.confApi.db.clube.campanha.dto.CampanhasAgrupadasDTO;
import com.confApi.db.clube.campanha.dto.RankingEntryDTO;
import com.confApi.db.clube.contabiliCampanha.ContabiliCampanha;
import com.confApi.db.clube.usuario.UsuarioClube;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.clube.Campanha.CampanhaApi;
import com.confApi.endPoints.clube.contabiliCampanha.ContabiliCampanhaApi;
import com.confApi.endPoints.clube.message.ResponseMessage;
import com.confApi.endPoints.clube.usuario.UsuarioClubeApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampanhaService {

    @Autowired
    private CampanhaApi campanhaApi;

    @Autowired
    private ContabiliCampanhaApi contabiliCampanhaApi;

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    @Autowired
    private UsuarioApi usuarioApi;

    @Autowired
    private CampanhaRegraRegularesService regraRegularesService;

    @Autowired
    private CampanhaRegraService regraService;

    public ResponseMessage create(Campanha campanha) {
        return campanhaApi.create(campanha);
    }

    public List<Campanha> getAll() {
        return campanhaApi.getAll();
    }

    public List<Campanha> getAllAtivas() {
        return campanhaApi.getAllAtivas();
    }

    public List<Campanha> getAllAtivasStatus1() {
        return campanhaApi.getAllAtivasStatus1();
    }

    public List<Campanha> getCampanhasAtivasHoje() {
        return campanhaApi.getCampanhasAtivasHoje();
    }

    public Campanha getById(Integer id) {
        return campanhaApi.getById(id);
    }

    public Campanha update(int id, Campanha campanha) {
        return campanhaApi.update(id, campanha);
    }


    /**** campanhas agrupadas ****/

    public CampanhasAgrupadasDTO getCampanhasAgrupadas(String loginUsuario) {
        CampanhasAgrupadasDTO resposta = new CampanhasAgrupadasDTO();
        try {
            List<Campanha> campanhasAtivas = campanhaApi.getCampanhasAtivasHoje();
            if (campanhasAtivas == null || campanhasAtivas.isEmpty()) {
                return resposta;
            }

            List<UsuarioClube> todosUsuarios = usuarioClubeApi.getAll();
            Usuario usuarioLogado = usuarioApi.consultaUsuarioByLogin(loginUsuario);
            String nomeUnidade = getPerfilUsuario(usuarioLogado);

            // Regionais (tipo 2)
            resposta.setCampanhaRankingDTOsRegionais(
                    regraService.montarLista(campanhasAtivas, 2, nomeUnidade, todosUsuarios, usuarioLogado));

            // Mensais (tipo 0)
            resposta.setCampanhaRankingDTOsMensais(
                    regraService.montarLista(campanhasAtivas, 0, nomeUnidade, todosUsuarios, usuarioLogado));

            resposta.setCampanhaRankingDTOsRegulares(
                    regraRegularesService.montarListaRegulares(campanhasAtivas, 1, nomeUnidade, todosUsuarios, usuarioLogado));


            return resposta;

        } catch (Exception e) {
            e.printStackTrace();
            return resposta;
        }
    }

    private String getPerfilUsuario(com.confApi.db.confManager.usuario.Usuario usuarioLogado) {
        if (usuarioLogado == null) return null;
        if (usuarioLogado.getAgencia() != null && usuarioLogado.getUnidade() != null) {
            return usuarioLogado.getAgencia().getCodgUnidade().getNomeUnidade();
        } else if (usuarioLogado.getAgencia() == null && usuarioLogado.getUnidade() != null) {
            return usuarioLogado.getUnidade().getNomeUnidade();
        }
        return null;
    }



}
