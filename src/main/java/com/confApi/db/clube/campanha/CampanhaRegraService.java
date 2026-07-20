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
import java.util.stream.Collectors;

@Service
public class CampanhaRegraService {

    @Autowired
    private CampanhaApi campanhaApi;

    @Autowired
    private ContabiliCampanhaApi contabiliCampanhaApi;

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    @Autowired
    private UsuarioApi usuarioApi;

    List<CampanhaRankingDTO> montarLista(List<Campanha> campanhasAtivas, int flagTipoMercado,
                                         String nomeUnidade, List<UsuarioClube> todosUsuarios, Usuario usuarioLogado) {

        List<CampanhaRankingDTO> lista = new ArrayList<>();

        List<Campanha> campanhas = campanhasAtivas.stream()
                .filter(c -> filtrarCampanhas(c, flagTipoMercado, nomeUnidade))
                .collect(Collectors.toList());

        for (Campanha campanha : campanhas) {
            List<RankingEntryDTO> ranking = rankingToCampanhaList(campanha, todosUsuarios, usuarioLogado);
            if (ranking == null) {
                ranking = new ArrayList<>();
            }
            aplicarCapitalizacao(ranking);

            CampanhaRankingDTO dto = new CampanhaRankingDTO();
            dto.setCodgCampanha(campanha.getCodgCampanha());
            dto.setNomeCampanha(getTruncatedNomeCampanha(campanha.getNomeCampanha()));
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

            lista.add(dto);
        }

        return lista;
    }

    private void aplicarCapitalizacao(List<RankingEntryDTO> ranking) {
        if (ranking == null) {
            return;
        }
        for (RankingEntryDTO entry : ranking) {
            entry.setNomeAgencia(capitalizeFirstLetter(entry.getNomeAgencia()));
            entry.setNomeUsuario(capitalizeFirstLetter(entry.getNomeUsuario()));
        }
    }

    public String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String lowerCased = input.toLowerCase();
        return lowerCased.substring(0, 1).toUpperCase() + lowerCased.substring(1);
    }

    public String getTruncatedNomeCampanha(String nomeCampanha) {
        if (nomeCampanha == null) {
            return null;
        }
        if (nomeCampanha.length() > 35) {
            return nomeCampanha.substring(0, 35);
        }
        return nomeCampanha;
    }

    private boolean filtrarCampanhas(Campanha c, int flagTipoMercado, String nomeUnidade) {
        return c.getFlagStatusCampanha() == 1
                && c.getFlagTipoMercado() == flagTipoMercado
                && (c.getFlagTipoPublico() == 1 || nomeUnidade == null || c.getDescUnidade() == null
                || Arrays.stream(c.getDescUnidade().split(",\\s*"))
                .anyMatch(unidade -> unidade.equalsIgnoreCase(nomeUnidade)))
                && (c.getFlagTipoPublico() == 0 || nomeUnidade == null || c.getDescUnidade() == null
                || Arrays.stream(c.getDescUnidade().split(",\\s*"))
                .anyMatch(unidade -> unidade.equalsIgnoreCase(nomeUnidade)));
    }



    public List<RankingEntryDTO> rankingToCampanhaList(Campanha campanha1, List<UsuarioClube> todosUsuarios, Usuario usuarioLogado) {
        List<RankingEntryDTO> rankingEntrys = null;
        RankingEntryDTO loggedUserPositionVisual;
        RankingEntryDTO loggedUserPosition;
        List<ContabiliCampanha> rankin1 = contabiliCampanhaApi
                .getRanking(campanha1.getCodgCampanha());

        // System.err.println("rankin1 : "+rankin1);
        if (rankin1 != null) {
            rankin1.stream().filter(entry -> existsByLogin(entry.getCodgUsuario(),todosUsuarios)).collect(Collectors.toList());
        }
        if (rankin1 != null) {
            // Validar a campanha e aplicar a regra específica
            List<ContabiliCampanha> campanhasOrdenadas = null;
            if (isCampanhaPorVendaCampanha(campanha1)) {
                //  System.err.println("entrou 1");
                campanhasOrdenadas = findRankingByAgencias(rankin1);
            } else if (isCampanhaPorBilhetesToAgencia(campanha1)) {
                //   System.err.println("entrou 2");
                campanhasOrdenadas = findRankingByAgenciasBilhete(rankin1);
            } else if (isCampanhaPorBilhetesCampanhaNotVenda(campanha1)) {
                //  System.err.println("entrou 3");
                campanhasOrdenadas = findRankingByBilhetes(rankin1);
            } else if (isCampanhaPorBilhetes(campanha1)) {
                //  System.err.println("entrou 4");
                campanhasOrdenadas = findRankingByBilhetes(rankin1);
            } else if (isCampanhaPorBilhetesCampanha(campanha1)) {
                //   System.err.println("entrou 5");
                campanhasOrdenadas = findRankingByBilhetes(rankin1);
            } else if (isCampanhaPorBilhetes(campanha1) || isCampanhaPorAgenciaVenda(campanha1)) {
                //  System.err.println("entrou 6");
                campanhasOrdenadas = findRankingByBilhetes(rankin1);
            } else if (isCampanhaPorAgencia(campanha1)) {
                //  System.err.println("entrou 7");
                campanhasOrdenadas = findRankingByAgencias(rankin1);
            } else if (isCampanhaPorValorAgencia(campanha1)) {
                //  System.err.println("entrou 8");
                campanhasOrdenadas = findRankingByValorAgencia(rankin1);
            } else if (isCampanhaPorValorUsuario(campanha1)) {
                // System.err.println("entrou 9");
                campanhasOrdenadas = findRankingByValorUsuario(rankin1);
            } else if (isCampanhaPorValorUsuarioVenda(campanha1)) {
                //  System.err.println("entrou 10 " +rankin1.size());
                campanhasOrdenadas = findRankingByValorUsuario(rankin1);
            } else if (isCampanhaPorValorUsuarioTarifa(campanha1)) {
                //  System.err.println("entrou 11 " +rankin1.size());
                campanhasOrdenadas = findRankingByValorUsuario(rankin1);
            }

            if (campanhasOrdenadas != null) {
                //  System.err.println("campanhasOrdenadas : "+campanhasOrdenadas.size());
                rankingEntrys = processarRanking(campanhasOrdenadas, campanha1,usuarioLogado);
                //  System.err.println("rankingEntrys : "+rankingEntrys);
                loggedUserPosition = determineUserPosition(campanhasOrdenadas, campanha1,usuarioLogado); // Defina a posição do usuário logado antes de aplicar o filtro
                loggedUserPositionVisual = determineUserPosition(campanhasOrdenadas, campanha1,usuarioLogado);
                // System.err.println("loggedUserPosition aki : "+loggedUserPosition);
                //  System.err.println("loggedUserPositionVisual aki : "+loggedUserPositionVisual);
                // Defina a posição do usuário logado antes de aplicar o filtro

            } else {
                //System.out.println("Nenhum dado encontrado para a campanha selecionada. 1 " + campanha1.getCodgCampanha());
            }
        } else {
            //System.out.println("Nenhum dado encontrado para a campanha selecionada. 2 " + campanha1.getCodgCampanha());
        }

        return rankingEntrys;
    }

    public List<RankingEntryDTO> processarRanking(List<ContabiliCampanha> campanhasOrdenadas, Campanha campanha, Usuario usuarioLogado) {
        List<RankingEntryDTO> allEntries = new ArrayList<>();
        List<RankingEntryDTO> rankingEntries;
        int position = 1;

        String loggedUser = null;
        String loggedUserAgencia = null;

        if (usuarioLogado.getAgencia() != null) {
            loggedUser = usuarioLogado.getLoginUsuario();
            loggedUserAgencia = usuarioLogado.getAgencia().getNomeAgencia();
        } else {

        }
        if (isCampanhaPorVendaCampanha(campanha)) {
            // System.out.println("CAIU 1 : " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                int total = qtdVenda + qtdBilhetes;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorBilhetesToAgencia(campanha)) {
            // System.out.println("CAIU 2 : " + campanha.getNomeCampanha());
            Map<String, Integer> agenciaVendasMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String nomeAgencia = contabiliCampanha.getNomeAgencia() != null
                        ? contabiliCampanha.getNomeAgencia().toString() : "";
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;

                int total = qtdVenda + qtdBilhetes;
                //System.out.println("Processando nomeAgencia: " + nomeAgencia + ", total: " + total);
                agenciaVendasMap.put(nomeAgencia, agenciaVendasMap.getOrDefault(nomeAgencia, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedAgencias = new ArrayList<>(agenciaVendasMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(entry.getKey());
                rankingEntry.setTotalVendas(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        } else if (isCampanhaPorValorUsuarioVenda(campanha)) {
            // System.out.println("CAIU 3 : " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                //     System.err.println("contabiliCampanha.getCodgUsuario() : "+contabiliCampanha.getCodgUsuario());
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                int total = qtdVenda + qtdBilhetes;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorValorUsuarioTarifa(campanha)) {
            //  System.out.println("CAIU 3.3 : " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();

                int total = (int) (contabiliCampanha.getValor() != null ? contabiliCampanha.getValor() : 0.0);

                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorBilhetes(campanha) || isCampanhaPorBilhetesCampanhaNotVenda(campanha) || isCampanhaPorVenda(campanha)) {
            //  System.out.println("CAIU 4 " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + qtdBilhetes);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setNomeUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                if (entry != null && entry.getKey() != null) {
                    rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));
                } else {
                    // Trate o caso em que `entry` ou `entry.getKey()` é nulo
                    if (entry == null) {
                        //    System.out.println("entry is null");
                    } else if (entry.getKey() == null) {
                        //   System.out.println("entry.getKey() is null");
                    }
                }

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeUsuario().equalsIgnoreCase(loggedUser) || contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorAgencia(campanha) || isCampanhaPorAgenciaVenda(campanha)) {

            //  System.out.println("CAIU 5 " + campanha.getNomeCampanha());
            Map<String, Integer> agenciaVendasMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String nomeAgencia = contabiliCampanha.getNomeAgencia() != null
                        ? contabiliCampanha.getNomeAgencia().toString() : "";
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;

                int total = qtdVenda + qtdBilhetes;
                //System.out.println("Processando nomeAgencia: " + nomeAgencia + ", total: " + total);
                agenciaVendasMap.put(nomeAgencia, agenciaVendasMap.getOrDefault(nomeAgencia, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedAgencias = new ArrayList<>(agenciaVendasMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(entry.getKey());
                rankingEntry.setTotalVendas(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorValorAgencia(campanha)) {
            //  System.out.println("CAIU 6");
            Map<String, Double> agenciaValorMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgAgencia = contabiliCampanha.getNomeAgencia() != null ? contabiliCampanha.getNomeAgencia().toString() : "";
                double valor = contabiliCampanha.getValor() != null ? contabiliCampanha.getValor() : 0.0;
                agenciaValorMap.put(codgAgencia, agenciaValorMap.getOrDefault(codgAgencia, 0.0) + valor);
            }

            List<Map.Entry<String, Double>> sortedAgencias = new ArrayList<>(agenciaValorMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Double> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(loggedUserAgencia);
                rankingEntry.setTotalValor(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        } else if (isCampanhaPorValorUsuario(campanha)) {
            //  System.out.println("CAIU 7 " + campanha.getNomeCampanha());
            Map<String, Integer> usuarioValorMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                usuarioValorMap.put(codgUsuario, usuarioValorMap.getOrDefault(codgUsuario, 0) + qtdBilhetes);
            }

            List<Map.Entry<String, Integer>> sortedUsuarios = new ArrayList<>(usuarioValorMap.entrySet());
            sortedUsuarios.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsuarios) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setNomeUsuario(entry.getKey());
                rankingEntry.setTotalValor(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeUsuario().equalsIgnoreCase(loggedUser) || contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        }

        // Filtrar resultados para exibir apenas as entradas relevantes para o usuário logado
        if (campanha.getFlagTipoMercado() == 0) {
            //  System.out.println("Campanha 1");
            return rankingEntries = filterEntriesForLoggedUser(allEntries, campanha.getQuantidadeTopResultado(), usuarioLogado);
            //ramking.getNomeUnidade().equalsIgnoreCase(Util.getUsuarioLogado().getAgencia().getCodgUnidade().getNomeUnidade())
        } else if (campanha.getFlagTipoMercado() == 1) {
            return rankingEntries = filterEntriesForLoggedAgencia(allEntries, campanha.getQuantidadeTopResultado(),usuarioLogado);
        } else {
            return rankingEntries = filterEntriesForRegional(allEntries, campanha.getQuantidadeTopResultado(),usuarioLogado);
        }

    }

    // Método para filtrar e adicionar a posição do usuário logado, se necessário
    private List<RankingEntryDTO> filterEntriesForLoggedUser(List<RankingEntryDTO> allEntries, Integer quantidadeTopResultado, Usuario usuarioLogado) {

        if (usuarioLogado.getAgencia() == null) {
            // Se for nulo, retorna todos os resultados

            return allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .collect(Collectors.toList());
        } else {
            String loggedUser = usuarioLogado.getLoginUsuario();
            String loggedUserAgencia = usuarioLogado.getAgencia().getNomeAgencia();

            boolean isInTopResults = allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .anyMatch(entry -> entry.getCodgUsuario() != null && entry.getCodgUsuario().equals(loggedUser)
                            || entry.getNomeAgencia() != null && entry.getNomeAgencia().equals(loggedUserAgencia));

            if (isInTopResults) {
                return allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .collect(Collectors.toList());
            } else {
                List<RankingEntryDTO> filteredEntries = allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .collect(Collectors.toList());

                filteredEntries.addAll(allEntries.stream()
                        .filter(entry -> entry.getCodgUsuario() != null && entry.getCodgUsuario().equals(loggedUser)
                                || entry.getNomeAgencia() != null && entry.getNomeAgencia().equals(loggedUserAgencia))
                        .collect(Collectors.toList()));

                return filteredEntries;
            }
        }
    }

    //Campanha
    private List<RankingEntryDTO> filterEntriesForLoggedAgencia(List<RankingEntryDTO> allEntries, Integer quantidadeTopResultado, Usuario usuarioLogado) {

        List<RankingEntryDTO> filteredEntries;
        if (usuarioLogado.getAgencia() == null) {
            // Se for nulo, retorna todos os resultados
            return allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .collect(Collectors.toList());
        } else {
            String loggedUserAgencia = usuarioLogado.getAgencia().getNomeAgencia();
            boolean isInTopResults = allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .anyMatch(entry -> entry.getCodgUsuario() != null && entry.getCodgUsuario().equals(loggedUserAgencia)
                            || entry.getNomeAgencia() != null && entry.getNomeAgencia().equals(loggedUserAgencia));

            if (isInTopResults) {
                return allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .collect(Collectors.toList());
            } else {
                filteredEntries = allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .collect(Collectors.toList());

                filteredEntries.addAll(allEntries.stream()
                        .filter(entry -> entry.getCodgUsuario() != null && entry.getCodgUsuario().equals(loggedUserAgencia)
                                || entry.getNomeAgencia() != null && entry.getNomeAgencia().equals(loggedUserAgencia))
                        .collect(Collectors.toList()));

                return filteredEntries;
            }
        }
    }

    //Reagional
    private List<RankingEntryDTO> filterEntriesForRegional(List<RankingEntryDTO> allEntries, Integer quantidadeTopResultado, Usuario usuarioLogado) {
        List<RankingEntryDTO> filteredEntries;
// Nome da unidade vinculada ao usuário
        if (usuarioLogado.getAgencia() == null) {
            // Se for nulo, retorna todos os resultados
            filteredEntries = allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .collect(Collectors.toList());
            if (quantidadeTopResultado != null && filteredEntries.size() > quantidadeTopResultado) {
                return filteredEntries.subList(0, quantidadeTopResultado);
            }

            return filteredEntries;
        } else {

            String nomeUnidade = usuarioLogado.getAgencia().getCodgUnidade().getNomeUnidade();

            // Filtrar as entradas que pertencem à unidade especificada
            boolean isInTopResults = allEntries.stream()
                    .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                    .anyMatch(entry -> entry.getNomeUnidade() != null && entry.getNomeUnidade().equals(nomeUnidade));

            if (isInTopResults) {
                // System.out.println("caiu aqui 1");
                filteredEntries = allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .filter(entry -> entry.getNomeUnidade() != null && entry.getNomeUnidade().equalsIgnoreCase(nomeUnidade))
                        .collect(Collectors.toList());

                filteredEntries.addAll(allEntries.stream()
                        .filter(RankingEntryDTO::getIsUserLogged)
                        .collect(Collectors.toList()));
            } else {
                // System.out.println("caiu aqui 2");
                return allEntries.stream()
                        .limit(quantidadeTopResultado != null ? quantidadeTopResultado : allEntries.size())
                        .collect(Collectors.toList());

                // Limitar a quantidade de resultados conforme especificado
            }
            return filteredEntries;
        }
    }

    public RankingEntryDTO determineUserPosition(List<ContabiliCampanha> campanhasOrdenadas, Campanha campanha, Usuario usuarioLogado) {
        int position = 1;
        String loggedUser = null;
        String loggedUserAgencia = null;

        if (usuarioLogado.getAgencia() == null) {

        } else {

            loggedUser = usuarioLogado.getLoginUsuario();
            loggedUserAgencia = usuarioLogado.getAgencia().getNomeAgencia();
        }

        List<RankingEntryDTO> allEntries = new ArrayList<>();

        if (isCampanhaPorVendaCampanha(campanha)) {
            // System.out.println(" outro CAIU 0 " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                int total = qtdVenda + qtdBilhetes;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorBilhetesToAgencia(campanha)) {
            // System.out.println("outro CAIU " + campanha.getNomeCampanha());
            Map<String, Integer> agenciaVendasMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String nomeAgencia = contabiliCampanha.getNomeAgencia() != null
                        ? contabiliCampanha.getNomeAgencia().toString() : "";
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;

                int total = qtdVenda + qtdBilhetes;
                //System.out.println("Processando nomeAgencia: " + nomeAgencia + ", total: " + total);
                agenciaVendasMap.put(nomeAgencia, agenciaVendasMap.getOrDefault(nomeAgencia, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedAgencias = new ArrayList<>(agenciaVendasMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(entry.getKey());
                rankingEntry.setTotalVendas(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        } else if (isCampanhaPorValorUsuarioVenda(campanha)) {
            // System.out.println("outro CAIU 0 " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                int total = qtdVenda + qtdBilhetes;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorBilhetes(campanha) || isCampanhaPorBilhetesCampanhaNotVenda(campanha) || isCampanhaPorVenda(campanha)) {
            //  System.out.println("outro CAIU 1 " + campanha.getNomeCampanha());
            Map<String, Integer> userBilhetesMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                userBilhetesMap.put(codgUsuario, userBilhetesMap.getOrDefault(codgUsuario, 0) + qtdBilhetes);
            }

            List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userBilhetesMap.entrySet());
            sortedUsers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsers) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setNomeUsuario(entry.getKey());
                rankingEntry.setTotalBilhetes(entry.getValue());
                if (entry != null && entry.getKey() != null) {
                    rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));
                } else {
                    // Trate o caso em que `entry` ou `entry.getKey()` é nulo
                    if (entry == null) {
                        //     System.out.println("entry is null");
                    } else if (entry.getKey() == null) {
                        //      System.out.println("entry.getKey() is null");
                    }
                }

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeUsuario().equalsIgnoreCase(loggedUser) || contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorAgencia(campanha) || isCampanhaPorAgenciaVenda(campanha)) {

            //  System.out.println("outro CAIU 2 " + campanha.getNomeCampanha());
            Map<String, Integer> agenciaVendasMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String nomeAgencia = contabiliCampanha.getNomeAgencia() != null
                        ? contabiliCampanha.getNomeAgencia().toString() : "";
                int qtdVenda = contabiliCampanha.getQtdVenda() != null ? contabiliCampanha.getQtdVenda() : 0;
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;

                int total = qtdVenda + qtdBilhetes;
                //System.out.println("Processando nomeAgencia: " + nomeAgencia + ", total: " + total);
                agenciaVendasMap.put(nomeAgencia, agenciaVendasMap.getOrDefault(nomeAgencia, 0) + total);
            }

            List<Map.Entry<String, Integer>> sortedAgencias = new ArrayList<>(agenciaVendasMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(entry.getKey());
                rankingEntry.setTotalVendas(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }

        } else if (isCampanhaPorValorAgencia(campanha)) {
            // System.out.println("outro CAIU 3");
            Map<String, Double> agenciaValorMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgAgencia = contabiliCampanha.getNomeAgencia() != null ? contabiliCampanha.getNomeAgencia().toString() : "";
                double valor = contabiliCampanha.getValor() != null ? contabiliCampanha.getValor() : 0.0;
                agenciaValorMap.put(codgAgencia, agenciaValorMap.getOrDefault(codgAgencia, 0.0) + valor);
            }

            List<Map.Entry<String, Double>> sortedAgencias = new ArrayList<>(agenciaValorMap.entrySet());
            sortedAgencias.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Double> entry : sortedAgencias) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgAgencia(entry.getKey());
                rankingEntry.setNomeAgencia(loggedUserAgencia);
                rankingEntry.setTotalValor(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUserAgencia));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeAgencia().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        } else if (isCampanhaPorValorUsuario(campanha)) {
            // System.out.println("outro CAIU 4 " + campanha.getNomeCampanha());
            Map<String, Integer> usuarioValorMap = new HashMap<>();
            for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                String codgUsuario = contabiliCampanha.getCodgUsuario();
                int qtdBilhetes = contabiliCampanha.getQtdBilhetes() != null ? contabiliCampanha.getQtdBilhetes() : 0;
                usuarioValorMap.put(codgUsuario, usuarioValorMap.getOrDefault(codgUsuario, 0) + qtdBilhetes);
            }

            List<Map.Entry<String, Integer>> sortedUsuarios = new ArrayList<>(usuarioValorMap.entrySet());
            sortedUsuarios.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (Map.Entry<String, Integer> entry : sortedUsuarios) {
                RankingEntryDTO rankingEntry = new RankingEntryDTO();
                rankingEntry.setPosition(position);
                rankingEntry.setCodgUsuario(entry.getKey());
                rankingEntry.setNomeUsuario(entry.getKey());
                rankingEntry.setTotalValor(entry.getValue());
                rankingEntry.setTotalBilhetes(entry.getValue());
                rankingEntry.setIsUserLogged(entry.getKey().equals(loggedUser));

                // Adicionando informações de unidade, se disponíveis
                for (ContabiliCampanha contabiliCampanha : campanhasOrdenadas) {
                    if (contabiliCampanha.getNomeUsuario().equalsIgnoreCase(loggedUser) || contabiliCampanha.getCodgUsuario().equals(entry.getKey()) && contabiliCampanha.getCodgUnidade() != null) {
                        rankingEntry.setCodgUnidade(contabiliCampanha.getCodgUnidade());
                        rankingEntry.setNomeUnidade(contabiliCampanha.getNomeUnidade());
                        rankingEntry.setNomeUsuario(contabiliCampanha.getNomeUsuario());
                        // Não sair do loop, pois queremos adicionar todas as correspondências
                    }
                }

                allEntries.add(rankingEntry);
                position++;
            }
        }
        // Procurar a posição do usuário logado na lista completa
        RankingEntryDTO userPosition = getLoggedUserPosition(allEntries,usuarioLogado);

        // Se não encontrado, defina como null ou um valor padrão
        if (userPosition == null) {
            // System.out.println("Usuário não encontrado no ranking atual.");
            return null; // ou retorne um RankingEntry com valores padrão, dependendo do seu caso de uso
        }

        return userPosition;
    }

    public RankingEntryDTO getLoggedUserPosition(List<RankingEntryDTO> rankingEntries, Usuario usuarioLogado) {

        if (usuarioLogado.getAgencia() != null) {

            String loggedUser = usuarioLogado.getLoginUsuario();
            String loggedUserAgencia = usuarioLogado.getAgencia().getNomeAgencia();

            for (RankingEntryDTO entry : rankingEntries) {
                if ((entry.getCodgUsuario() != null && entry.getCodgUsuario().equals(loggedUser))
                        || (entry.getNomeAgencia() != null && entry.getNomeAgencia().equals(loggedUserAgencia))) {
                    return entry;
                }
            }
        }
        return null;
    }

    // Método para buscar e ordenar o ranking por quantidade de vendas (agências)
    public List<ContabiliCampanha> findRankingByAgenciasBilhete(List<ContabiliCampanha> campanhas) {
        if (campanhas != null) {
            return campanhas.stream()
                    .sorted(Comparator.comparing(ContabiliCampanha::getQtdBilhetes,
                            Comparator.nullsLast(Integer::compareTo)).reversed())
                    .collect(Collectors.toList());
        }
        return null;
    }

    // Método para buscar e ordenar o ranking por quantidade de bilhetes emitidos
    public List<ContabiliCampanha> findRankingByBilhetes(List<ContabiliCampanha> campanhas) {
        if (campanhas != null) {
            return campanhas.stream()
                    .sorted(Comparator.comparing(ContabiliCampanha::getQtdBilhetes,
                            Comparator.nullsLast(Integer::compareTo)).reversed())
                    .collect(Collectors.toList());
        }
        return null;
    }

    // Método para buscar e ordenar o ranking por quantidade de vendas (agências)
    public List<ContabiliCampanha> findRankingByAgencias(List<ContabiliCampanha> campanhas) {
        if (campanhas != null) {
            return campanhas.stream()
                    .sorted(Comparator.comparing(ContabiliCampanha::getQtdVenda,
                            Comparator.nullsLast(Integer::compareTo)).reversed())
                    .collect(Collectors.toList());
        }
        return null;
    }

    // Método para buscar e ordenar o ranking por valor (agências)
    public List<ContabiliCampanha> findRankingByValorAgencia(List<ContabiliCampanha> campanhas) {
        if (campanhas != null) {
            return campanhas.stream()
                    .sorted(Comparator.comparing(ContabiliCampanha::getValor,
                            Comparator.nullsLast(Double::compareTo)).reversed())
                    .collect(Collectors.toList());
        }
        return null;
    }

    // Método para buscar e ordenar o ranking por valor (usuários)
    public List<ContabiliCampanha> findRankingByValorUsuario(List<ContabiliCampanha> campanhas) {
        if (campanhas != null) {
            return campanhas.stream()
                    .sorted(Comparator.comparing(ContabiliCampanha::getValor,
                            Comparator.nullsLast(Double::compareTo)).reversed())
                    .collect(Collectors.toList());
        }
        return null;
    }

    private boolean isCampanhaPorBilhetesToAgencia(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 0
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 1
                && campanha.getFlagContabilEmissor() == 0
                && campanha.getFlagTipoContabilValorPago() == 0
                && campanha.getValorPago() == 0.0;
    }

    // Métodos de validação das campanhas
    private boolean isCampanhaPorVenda(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 0
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getFlagTipoContabilValorPago() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorBilhetes(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getFlagTipoContabilValorPago() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorVendaCampanha(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 0
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getFlagTipoContabilValorPago() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorBilhetesCampanha(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getFlagTipoContabilValorPago() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorBilhetesCampanhaNotVenda(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 0
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getFlagTipoContabilValorPago() == 0;
    }

    private boolean isCampanhaPorAgencia(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 1
                && campanha.getFlagContabilEmissor() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorAgenciaVenda(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 0
                && campanha.getFlagContabilAgencia() == 1
                && campanha.getFlagContabilEmissor() == 0
                && campanha.getValorPago() == 0.0;
    }

    private boolean isCampanhaPorValorAgencia(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 1
                && campanha.getFlagContabilEmissor() == 0
                && campanha.getFlagTipoContabilValorPago() == 1
                && campanha.getValorPago() > 0.0;
    }

    private boolean isCampanhaPorValorUsuario(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 0
                && campanha.getFlagContabilBilhete() == 1
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagTipoContabilValorPago() == 1
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getValorPago() > 0.0;
    }

    private boolean isCampanhaPorValorUsuarioVenda(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 1
                && campanha.getFlagContabilTarifa() == 0
                && campanha.getFlagContabilBilhete() == 0
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagTipoContabilValorPago() == 1
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getValorPago() > 0.0;
    }

    private boolean isCampanhaPorValorUsuarioTarifa(Campanha campanha) {
        return campanha.getFlagTipoContabilizaVendas() == 0
                && campanha.getFlagContabilTarifa() == 1
                && campanha.getFlagContabilBilhete() == 0
                && campanha.getFlagContabilAgencia() == 0
                && campanha.getFlagTipoContabilValorPago() == 1
                && campanha.getFlagContabilEmissor() == 1
                && campanha.getValorPago() > 0.0;
    }

    public boolean existsByLogin(String login, List<UsuarioClube> todosUsuarios) {
        if (login == null || todosUsuarios == null) {
            return false;
        }
        for (UsuarioClube usuarioClube : todosUsuarios) {
            if (usuarioClube.getLoginUsuario() != null
                    && usuarioClube.getLoginUsuario().equalsIgnoreCase(login)) {
                return true;   // pode retornar direto ao achar
            }
        }
        return false;
    }



}
