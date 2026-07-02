package com.confApi.db.clube.campanha;

import com.confApi.db.clube.campanha.dto.CampanhaRankingDTO;
import com.confApi.db.clube.campanha.dto.RankingEntryDTO;
import com.confApi.db.clube.contabiliCampanha.ContabiliCampanha;
import com.confApi.db.clube.usuario.UsuarioClube;
import com.confApi.endPoints.clube.Campanha.CampanhaApi;
import com.confApi.endPoints.clube.contabiliCampanha.ContabiliCampanhaApi;
import com.confApi.endPoints.clube.message.ResponseMessage;
import com.confApi.endPoints.clube.usuario.UsuarioClubeApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    public List<CampanhaRankingDTO> getCampanhas(String loginUsuario) {
        return getPopularCampanhas(loginUsuario);
    }

    /***consulta de campanha***/

    public List<CampanhaRankingDTO> getPopularCampanhas(String loginUsuario) {
        try {
            // 1. Busca campanhas ativas
            List<Campanha> campanhasAtivas = campanhaApi.getCampanhasAtivasHoje();
            if (campanhasAtivas == null || campanhasAtivas.isEmpty()) {
                return new ArrayList<>();
            }

            // 2. Busca todos os usuários para validar existsByLogin
            List<UsuarioClube> todosUsuarios = usuarioClubeApi.getAll();

            // 3. Busca usuario logado para pegar unidade
            com.confApi.db.confManager.usuario.Usuario usuarioLogado = usuarioApi.consultaUsuarioByLogin(loginUsuario);
            String nomeUnidade = getPerfilUsuario(usuarioLogado);

            List<CampanhaRankingDTO> campanhas = new ArrayList<>();

            for (Campanha campanha : campanhasAtivas) {
                // 4. Ajuste IATA
                if (campanha.getIataCia() != null && campanha.getIataCia().equals("JJ")) {
                    campanha.setIataCia("LA");
                }

                // 5. Filtra por unidade
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

                // 6. Filtra só flagTipoMercado == 1 (Campanhas)
                if (isPossuiUnidade && campanha.getFlagTipoMercado() != null
                        && campanha.getFlagTipoMercado() == 1) {

                    // 7. Calcula ranking
                    List<ContabiliCampanha> contabilis = contabiliCampanhaApi
                            .getRanking(campanha.getCodgCampanha());

                    List<RankingEntryDTO> ranking = calculaRankingByRegistro(
                            contabilis, todosUsuarios, loginUsuario);

                    configuraRanking(campanha, ranking);

                    // 8. Monta DTO
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

            // 9. Ordena por tamanho do ranking
            campanhas.sort(Comparator.comparingInt(
                    (CampanhaRankingDTO c) -> c.getRankingCampanha().size()).reversed());

            return campanhas;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
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


    /*****/
   /* public List<String> listaUnidades(String unidadesString) {
        List<String> unidades = new ArrayList<>();
        String[] unidArgs = unidadesString.split(",");
        unidades = Arrays.asList(unidArgs);
        return unidades;
    }*/






}
