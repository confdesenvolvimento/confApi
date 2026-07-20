package com.confApi.db.clube.campanha;

import com.confApi.db.clube.campanha.dto.CampanhaRankingDTO;
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

@Service
public class CampanhaRegraRegularesService {

    @Autowired
    private CampanhaApi campanhaApi;

    @Autowired
    private ContabiliCampanhaApi contabiliCampanhaApi;

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    @Autowired
    private UsuarioApi usuarioApi;


    public List<CampanhaRankingDTO> montarListaRegulares(
            List<Campanha> campanhasAtivas,
            int flagTipoMercado,
            String nomeUnidade,
            List<UsuarioClube> todosUsuarios,
            Usuario usuarioLogado) {

        List<CampanhaRankingDTO> campanhas = new ArrayList<>();

        for (Campanha campanha : campanhasAtivas) {

            // Filtra por unidade
            if (campanha.getDescUnidade() == null || campanha.getDescUnidade().isEmpty()) {
                continue;
            }

            boolean isPossuiUnidade = false;
            if (nomeUnidade == null) {
                isPossuiUnidade = true;
            } else {
                List<String> unidades = Arrays.asList(campanha.getDescUnidade().split(","));
                for (String unidade : unidades) {
                    if (unidade.trim().equalsIgnoreCase(nomeUnidade.trim())) {
                        isPossuiUnidade = true;
                        break;
                    }
                }
            }

            // Filtra pelo tipo de mercado recebido
            if (isPossuiUnidade && campanha.getFlagTipoMercado() != null
                    && campanha.getFlagTipoMercado() == flagTipoMercado) {

                List<ContabiliCampanha> contabilis = contabiliCampanhaApi
                        .getRanking(campanha.getCodgCampanha());

                List<RankingEntryDTO> ranking = calculaRankingByRegistro(
                        contabilis, todosUsuarios, usuarioLogado.getLoginUsuario());

                configuraRanking(campanha, ranking);

                CampanhaRankingDTO dto = new CampanhaRankingDTO();
                dto.setCodgCampanha(campanha.getCodgCampanha());
                dto.setNomeCampanha(campanha.getNomeCampanha());
                dto.setTituloCampanha(campanha.getTituloCampanha());
                dto.setDescricaoCampanha(campanha.getDescricaoCampanha());
                dto.setRegrasCampanha(campanha.getRegrasCampanha());
                dto.setValidadeInicio(campanha.getValidadeInicio());
                dto.setValidadeFinal(campanha.getValidadeFinal());
                dto.setIataCia(campanha.getIataCia());
                dto.setArquivoAnexo(campanha.getArquivoAnexo());
                dto.setValorPago(campanha.getValorPago());
                dto.setFlagTipoMercado(campanha.getFlagTipoMercado());
                dto.setFlagStatusCampanha(campanha.getFlagStatusCampanha());
                dto.setDescUnidade(campanha.getDescUnidade());
                dto.setRankingCampanha(ranking);

                campanhas.add(dto);
            }
        }

        // Ordena por tamanho do ranking
        campanhas.sort(Comparator.comparingInt(
                (CampanhaRankingDTO c) -> c.getRankingCampanha().size()).reversed());

        return campanhas;
    }

    private String getPerfilUsuario(Usuario usuarioLogado) {
        if (usuarioLogado == null) return null;
        if (usuarioLogado.getAgencia() != null && usuarioLogado.getUnidade() != null) {
            return usuarioLogado.getAgencia().getCodgUnidade().getNomeUnidade();
        } else if (usuarioLogado.getAgencia() == null && usuarioLogado.getUnidade() != null) {
            return usuarioLogado.getUnidade().getNomeUnidade();
        }
        return null;
    }

    private List<RankingEntryDTO> calculaRankingByRegistro(
            List<ContabiliCampanha> contabilis,
            List<UsuarioClube> todosUsuarios,
            String loginUsuario) {

        Map<String, RankingEntryDTO> rankingMap = new HashMap<>();

        if (contabilis != null) {
            for (ContabiliCampanha c : contabilis) {
                if (c == null) continue;

                // Valida se login existe na base
                boolean exists = todosUsuarios.stream()
                        .anyMatch(u -> u.getLoginUsuario()
                                .equalsIgnoreCase(c.getCodgUsuario()));
                if (!exists) continue;

                if (c.getQtdVenda() == null) c.setQtdVenda(0);
                if (c.getQtdBilhetes() == null) c.setQtdBilhetes(0);
                if (c.getValor() == null) c.setValor(0.0);

                String key = c.getCodgUsuario() + "-" + c.getCodgCompanha();
                RankingEntryDTO entry = rankingMap.get(key);

                if (entry == null) {
                    entry = new RankingEntryDTO();
                    entry.setCodgUsuario(c.getCodgUsuario());
                    if (c.getCodgAgencia() != null) {
                        entry.setCodgAgencia(c.getCodgAgencia().toString());
                    }
                    entry.setNomeAgencia(c.getNomeAgencia());
                    entry.setNomeUnidade(c.getNomeUnidade());
                    entry.setCodgUnidade(c.getCodgUnidade());
                    entry.setNomeUsuario(c.getNomeUsuario());
                    entry.setTotalVendas(0);
                    entry.setTotalBilhetes(0);
                    entry.setTotalValor(0.0);
                    entry.setIsUserLogged(
                            c.getCodgUsuario().equalsIgnoreCase(loginUsuario));
                    rankingMap.put(key, entry);
                }

                entry.setTotalVendas(entry.getTotalVendas() + c.getQtdVenda());
                entry.setTotalBilhetes(entry.getTotalBilhetes() + c.getQtdBilhetes());
                entry.setTotalValor(entry.getTotalValor() + c.getValor());
                entry.setTotalValorCount((int) Math.round(entry.getTotalValor()));
            }
        }

        return new ArrayList<>(rankingMap.values());
    }

    private void configuraRanking(Campanha campanha, List<RankingEntryDTO> ranking) {
        for (RankingEntryDTO entry : ranking) {
            if (campanha.getFlagContabilAgencia() != null
                    && campanha.getFlagContabilAgencia() == 1) {
                entry.setNomeExibicaoRanking(entry.getNomeAgencia());
            } else if (campanha.getFlagContabilEmissor() != null
                    && campanha.getFlagContabilEmissor() == 1) {
                entry.setNomeExibicaoRanking(entry.getNomeUsuario());
            } else {
                entry.setNomeExibicaoRanking(entry.getNomeAgencia());
            }

            if (campanha.getFlagContabilBilhete() != null
                    && campanha.getFlagContabilBilhete() == 1) {
                entry.setContador(entry.getTotalBilhetes());
            } else if (campanha.getFlagTipoContabilizaVendas() != null
                    && campanha.getFlagTipoContabilizaVendas() == 1) {
                entry.setContador(entry.getTotalVendas());
            } else if (campanha.getFlagContabilTarifa() != null
                    && campanha.getFlagContabilTarifa() == 1) {
                entry.setContador(entry.getTotalValorCount());
            }
        }

        Integer qtd = campanha.getQuantidadeTopResultado() != null
                ? campanha.getQuantidadeTopResultado() : 50;

        ranking.sort(Comparator.comparingInt(RankingEntryDTO::getContador).reversed());
        if (qtd < ranking.size()) {
            ranking.subList(qtd, ranking.size()).clear();
        }

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosition(i + 1);
        }
    }

}
