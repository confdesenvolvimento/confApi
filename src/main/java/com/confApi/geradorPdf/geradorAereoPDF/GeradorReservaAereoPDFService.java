package com.confApi.geradorPdf.geradorAereoPDF;

import com.confApi.db.confManager.assentoAereo.Assento;
import com.confApi.geradorPdf.aereo.ReservaAereoModelPDF;
import com.confApi.hub.aereo.BilheteModel;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.hub.aereo.ReservaValoresAereo;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Service
public class GeradorReservaAereoPDFService {

    public byte[] gerarPdfReserva(ReservaAereoModelPDF reserva) throws IOException, DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Font titleFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 8);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        // Seções
        document.add(criarHeader(reserva));
//        document.add(new Paragraph(" "));
        document.add(criarTabelaCabecalho(reserva));
//        document.add(new Paragraph(" "));
        document.add(criarTabelaPassageiros(reserva, normalFont, headerFont));
//        document.add(new Paragraph(" "));

        PdfPTable tabelaAssentos = criarTabelaAssentos(reserva, normalFont, headerFont);
        if (tabelaAssentos != null) {
            document.add(tabelaAssentos);
//            document.add(new Paragraph(" "));
        }

        document.add(criarTabelaVoos(reserva, normalFont, headerFont));
//        document.add(new Paragraph(" "));
        document.add(criarTabelaValores(reserva, normalFont, headerFont));
//        document.add(new Paragraph(" "));

        if (reserva.getPassageiros() != null) {
            boolean temBilhete = reserva.getPassageiros().stream()
                    .anyMatch(p -> p.getBilhetes() != null && !p.getBilhetes().isEmpty());

            if (temBilhete) {
                document.add(criarTabelaBilhetes(reserva, normalFont, headerFont));
//                document.add(new Paragraph(" "));
            }
        }

        PdfPTable tabelaRecebimentos = criarTabelaRecebimentos(reserva, normalFont, headerFont);
        if (tabelaRecebimentos != null) {
            document.add(tabelaRecebimentos);
        }

        document.close();
        return baos.toByteArray();
    }

    private PdfPTable criarHeader(ReservaAereoModelPDF reserva) throws IOException, BadElementException {

        PdfPTable headerTable = new PdfPTable(new float[]{2f, 0.01f, 6f, 0.01f, 2f});
        headerTable.setWidthPercentage(100);
        //headerTable.setWidths(new float[]{2, 6, 2});
        //System.err.println("reserva " + reserva.getAgencia());
        // Logo esquerda

        PdfPCell cellLogoEsq;

        String logoPath = reserva.getAgencia().getLogomarca();
        if (logoPath != null && logoPath.matches(".*\\.(png|jpg|jpeg|gif|bmp)$")) {
            try {
                Image logoEsq = Image.getInstance(logoPath);
                logoEsq.scaleToFit(150, 40);
                cellLogoEsq = new PdfPCell(logoEsq);
            } catch (Exception e) {
                // Se der erro ao carregar imagem, usa célula em branco
                cellLogoEsq = new PdfPCell();
            }
        } else {
            // Sem imagem → célula vazia com fundo branco
            cellLogoEsq = new PdfPCell();
            cellLogoEsq.setBackgroundColor(Color.WHITE);
        }
        /* Image logoEsq = Image.getInstance(reserva.getAgencia().getLogomarca());
        logoEsq.scaleToFit(150, 40);
        PdfPCell cellLogoEsq = new PdfPCell(logoEsq);*/
        cellLogoEsq.setBorder(Rectangle.NO_BORDER);
        cellLogoEsq.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogoEsq.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellLogoEsq.setPadding(6f);
        headerTable.addCell(cellLogoEsq);

        // separador vertical 1 (coluna estreita com cor)
        PdfPCell sep1 = new PdfPCell();
        sep1.setBorder(Rectangle.NO_BORDER);
        sep1.setBackgroundColor(Color.LIGHT_GRAY); // cor da "linha"
        sep1.setPadding(0f);
        // sep1.setFixedHeight(60f); // ajuste se necessário para casar com conteúdo
        headerTable.addCell(sep1);

        // Definição das fontes
        Font titleFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font userFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);

        // Título centralizado com quebra de linha e nome do usuário
        // Primeiro parágrafo (título)
        Paragraph titulo = new Paragraph("Reserva Aérea - Plano de Viagem", titleFont);
        titulo.setAlignment(Element.ALIGN_CENTER);

        // Segundo parágrafo (usuário) com espaçamento antes
        Paragraph usuario = new Paragraph("Usuário: " + reserva.getUsuarioCriacao().getNomeCompleto(), userFont);
        usuario.setAlignment(Element.ALIGN_CENTER);
        usuario.setSpacingBefore(6f); // distancia apenas do título

        usuario.add(Chunk.NEWLINE); // quebra de linha
        usuario.add(new Chunk("E-mail: " + reserva.getUsuarioCriacao().getEmail(), userFont));

        usuario.add(Chunk.NEWLINE); // quebra de linha
        usuario.add(new Chunk("Telefone: " + reserva.getUsuarioCriacao().getTelefone(), userFont));

        PdfPCell cellTitulo = new PdfPCell();
        cellTitulo.addElement(titulo);
        cellTitulo.addElement(usuario);
        cellTitulo.setBorder(Rectangle.NO_BORDER);
        //cellTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        //cellTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellTitulo.setPadding(6f);
        headerTable.addCell(cellTitulo);

        // separador vertical 2
        PdfPCell sep2 = new PdfPCell();
        sep2.setBorder(Rectangle.NO_BORDER);
        sep2.setBackgroundColor(Color.LIGHT_GRAY);
        sep2.setPadding(0f);
        //sep2.setFixedHeight(60f);
        headerTable.addCell(sep2);

        // Logo direita
        Image logoDir = Image.getInstance("https://cdn.westonline.tur.br/confianca.tur/img/cia_short/" + reserva.getCodgCompanhiaAerea().getIataCia() + ".png");
        logoDir.scaleToFit(60, 40);
        PdfPCell cellLogoDir = new PdfPCell(logoDir);
        cellLogoDir.setBorder(Rectangle.NO_BORDER);
        cellLogoDir.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogoDir.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellLogoDir.setPadding(6f);
        headerTable.addCell(cellLogoDir);

        // wrapper com borda externa (4 lados)
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        PdfPCell wrapperCell = new PdfPCell(headerTable);
        wrapperCell.setBorder(Rectangle.NO_BORDER);  // borda nos 4 lados
        wrapperCell.setPadding(0f);
        wrapper.addCell(wrapperCell);

        return wrapper;

    }

    private PdfPTable criarTabelaCabecalho(ReservaAereoModelPDF reserva) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Font normalFont = new Font(Font.HELVETICA, 8);
        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);

        // título na primeira linha ocupando todas as colunas
        PdfPCell tituloCell = new PdfPCell(new Phrase("Reserva", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setColspan(7); // ocupa todas as 7 colunas
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setBackgroundColor(new Color(245, 245, 245));
        tituloCell.setPadding(5f);
        PdfPTable tableHeader = new PdfPTable(new float[]{2, 1, 2, 2, 2, 2, 2});
        tableHeader.setWidthPercentage(100);
        // Adiciona título como uma linha separada
        tableHeader.addCell(tituloCell);

        tableHeader.addCell(getHeaderCell("Localizador"));
        tableHeader.addCell(getHeaderCell("Status"));
        tableHeader.addCell(getHeaderCell("Criada em"));

        // Aqui a lógica para o nome da quarta coluna
        String colunaStatus;
        switch (reserva.getStatusReserva()) {
            case "Ativa":
                colunaStatus = "Prazo";
                break;
            case "Emitida":
                colunaStatus = "Emitida Em";
                break;
            case "Cancelada":
                colunaStatus = "Cancelada Em";
                break;
            default:
                colunaStatus = "Data";
        }
        tableHeader.addCell(getHeaderCell(colunaStatus));

        tableHeader.addCell(getHeaderCell("Criado por"));
        tableHeader.addCell(getHeaderCell("Agência"));
        tableHeader.addCell(getHeaderCell("Sistema"));

        tableHeader.addCell(getCell(reserva.getLocalizador(), smallFont));
        tableHeader.addCell(getCell(reserva.getStatusReserva(), smallFont));
        tableHeader.addCell(getCell(sdf.format(reserva.getDataCriacao()), smallFont));


        // Aqui você pode escolher que data vai aparecer na 4ª coluna
        if ("Ativa".equals(reserva.getStatusReserva())) {
            tableHeader.addCell(getCell(sdf.format(reserva.getPrazoReserva()), smallFont));
        } else if ("Emitida".equals(reserva.getStatusReserva())) {
            tableHeader.addCell(getCell(sdf.format(reserva.getDataEmissao()), smallFont));
        } else if ("Cancelada".equals(reserva.getStatusReserva())) {
            tableHeader.addCell(getCell(sdf.format(reserva.getDataCancelamento()), smallFont));
        } else {
            tableHeader.addCell(getCell("-", smallFont));
        }

        tableHeader.addCell(getCell(reserva.getUsuarioCriacao().getLoginUsuario(), smallFont));
        tableHeader.addCell(getCell(reserva.getAgencia().getNomeAgencia(), smallFont));

        String sistema = "wooba".equalsIgnoreCase(reserva.getSistema())
                ? "Portal do Agente"
                : reserva.getSistema();

        tableHeader.addCell(getCell(sistema, smallFont));

        return tableHeader;
    }

    private PdfPTable criarTabelaPassageiros(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) {

        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);

        // Título acima da tabela
        PdfPCell tituloCell2 = new PdfPCell(new Phrase("Passageiros", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell2.setBackgroundColor(new Color(245, 245, 245)); // cor #a1b1c5
        tituloCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell2.setPadding(6f);
        tituloCell2.setColspan(11); // tabela terá 11 colunas

        PdfPTable tablePass = new PdfPTable(5);
        tablePass.setWidthPercentage(100);

        // Adiciona título como uma linha separada
        tablePass.addCell(tituloCell2);

        tablePass.addCell(getHeaderCell("Tipo Passageiro"));
        tablePass.addCell(getHeaderCell("Sobrenome"));
        tablePass.addCell(getHeaderCell("Nome"));
        tablePass.addCell(getHeaderCell("Sexo"));
        tablePass.addCell(getHeaderCell("Data Nascimento"));

        for (PassageiroModel pass : reserva.getPassageiros()) {
            tablePass.addCell(getCell(pass.getFaixaEtaria(), smallFont));
            tablePass.addCell(getCell(pass.getSobrenome(), smallFont));
            tablePass.addCell(getCell(pass.getNome(), smallFont));
            tablePass.addCell(getCell(pass.getSexo(), smallFont));
            tablePass.addCell(getCell(pass.getNascimento().toString(), smallFont));
        }

        return tablePass;
    }

    private PdfPTable criarTabelaAssentos(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) {
        // Verifica se existem trechos com voos e assentos
        if (reserva.getTrechos() == null || reserva.getTrechos().isEmpty()) {
            return null;
        }
        boolean temAssentos = reserva.getTrechos().stream()
                .filter(t -> t.getVoos() != null)
                .flatMap(t -> t.getVoos().stream())
                .anyMatch(v -> v.getAssentos() != null && !v.getAssentos().isEmpty());

        if (!temAssentos) {
            return null;
        }

        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);

        // Título acima da tabela
        PdfPCell tituloCell = new PdfPCell(new Phrase("Assentos", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setBackgroundColor(new Color(245, 245, 245)); // cor #a1b1c5
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setPadding(6f);
        tituloCell.setColspan(10); // tabela terá 11 colunas

        // Cria a tabela com 10 colunas
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);

        // Adiciona título como uma linha separada
        table.addCell(tituloCell);

        // Cabeçalhos
        table.addCell(getHeaderCell("Companhia"));
        table.addCell(getHeaderCell("Voo"));
        table.addCell(getHeaderCell("Saída"));
        table.addCell(getHeaderCell("Chegada"));
        table.addCell(getHeaderCell("Origem"));
        table.addCell(getHeaderCell("Destino"));
        table.addCell(getHeaderCell("Passageiro"));
        table.addCell(getHeaderCell("Assento Reservado"));
        table.addCell(getHeaderCell("Valor"));
        table.addCell(getHeaderCell("Localizador Cia"));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Popula as linhas (trechos → voos → assentos)
        if (reserva.getTrechos() != null) {
            for (TrechoReserva trecho : reserva.getTrechos()) {
                if (trecho.getVoos() != null) {
                    for (Voo voo : trecho.getVoos()) {
                        if (voo.getAssentos() != null) {
                            for (Assento assento : voo.getAssentos()) {

                                // Adiciona +4 horas em Partida e Chegada
                                Calendar cal = Calendar.getInstance();

                                cal.setTime(voo.getDataPartida());
                                cal.add(Calendar.HOUR_OF_DAY, 0);
                                String partidaAjustada = sdf.format(cal.getTime());

                                cal.setTime(voo.getDataChegada());
                                cal.add(Calendar.HOUR_OF_DAY, 0);
                                String chegadaAjustada = sdf.format(cal.getTime());

                                table.addCell(getCell(voo.getCiaMandatoria().getDescricao(), smallFont));
                                table.addCell(getCell(voo.getCiaMandatoria().getCodigoIata() + " - " + voo.getNumeroVoo(), smallFont));
                                table.addCell(getCell(partidaAjustada, smallFont));
                                table.addCell(getCell(chegadaAjustada, smallFont));
                                table.addCell(getCell(voo.getOrigem().getDescricao(), smallFont));
                                table.addCell(getCell(voo.getDestino().getDescricao(), smallFont));
                                table.addCell(getCell(assento.getPassageiro(), smallFont));
                                table.addCell(getCell(assento.getAssentoLinha() + assento.getAssentoColuna(), smallFont));

                                // Valor formatado em moeda
                                /* String valorFmt = assento.getValor() != null
                                        ? String.format("R$ %.2f", assento.getValor())
                                        : "";
                                table.addCell(getCell(valorFmt, smallFont));*/
                                table.addCell(getCell(
                                        assento.getValor() != null
                                                ? String.format("R$ %.2f", assento.getValor())
                                                : "-", smallFont));

                                table.addCell(getCell(voo.getLocalizadorCia(), smallFont));
                            }
                        }
                    }
                }
            }
        }

        return table;
    }

    private PdfPTable criarTabelaVoos(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) {

        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);

        // Título acima da tabela
        PdfPCell tituloCell = new PdfPCell(new Phrase("Informações do Voo", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setBackgroundColor(new Color(245, 245, 245)); // cor #a1b1c5
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setPadding(6f);
        tituloCell.setColspan(8); // tabela terá 11 colunas

        PdfPTable tableVoo = new PdfPTable(new float[]{2, 1, 2, 2, 2, 2, 1, 1});
        tableVoo.setWidthPercentage(100);
        // Adiciona título como uma linha separada acima
        tableVoo.addCell(tituloCell);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        String[] headers = {"Companhia", "Voo", "Saída", "Chegada", "Origem", "Destino", "Aeronave", "Duração"};
        for (String h : headers) {
            tableVoo.addCell(getHeaderCell(h));
        }

        for (TrechoReserva t : reserva.getTrechos()) {
            for (Voo v : t.getVoos()) {

                // Adiciona +4 horas em Partida e Chegada
                Calendar cal = Calendar.getInstance();

                cal.setTime(v.getDataPartida());
                cal.add(Calendar.HOUR_OF_DAY, 0);
                String partidaAjustada = sdf.format(cal.getTime());

                cal.setTime(v.getDataChegada());
                cal.add(Calendar.HOUR_OF_DAY, 0);
                String chegadaAjustada = sdf.format(cal.getTime());

                tableVoo.addCell(getCell(v.getCiaMandatoria().getDescricao(), smallFont));
                tableVoo.addCell(getCell(v.getNumeroVoo(), smallFont));
                tableVoo.addCell(getCell(partidaAjustada, smallFont));
                tableVoo.addCell(getCell(chegadaAjustada, smallFont));
                tableVoo.addCell(getCell(v.getOrigem().getDescricao(), smallFont));
                tableVoo.addCell(getCell(v.getDestino().getDescricao(), smallFont));
                tableVoo.addCell(getCell(v.getEquipamento(), smallFont));
                tableVoo.addCell(getCell(v.getDuracao(), smallFont));
            }
        }

        return tableVoo;
    }

    private PdfPTable criarTabelaValores(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) {
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);
        Font boldFont = new Font(Font.HELVETICA, 6f, Font.BOLD, Color.BLACK);

        // Flags para saber se deve exibir a coluna
        boolean temEmbarque = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getValorTaxaEmbarque() != null && v.getValorTaxaEmbarque() > 0);

        boolean temAssento = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getTaxaAssento() != null && v.getTaxaAssento() > 0);

        boolean temDU = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getTaxaDu() != null && v.getTaxaDu().doubleValue() > 0);

        boolean temRAV = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getTaxaRav() != null && v.getTaxaRav().doubleValue() > 0);

        boolean temRC = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getTaxaRc() != null && v.getTaxaRc().doubleValue() > 0);

        boolean temTaxaCombustivel = reserva.getPassageiros().stream()
                .flatMap(p -> p.getValores().stream())
                .anyMatch(v -> v.getValorTxCombustivel() != null && v.getValorTxCombustivel() > 0);

        // Sempre existem essas colunas: Passageiro, Tarifa, Total
        int colCount = 3;

        if (temEmbarque) {
            colCount++;
        }
        if (temAssento) {
            colCount++;
        }
        if (temDU) {
            colCount++;
        }
        if (temRAV) {
            colCount++;
        }
        if (temRC) {
            colCount++;
        }
        if (temTaxaCombustivel) {
            colCount++;
        }

        // Agora sim cria a tabela com a quantidade real de colunas
        PdfPTable tableVal = new PdfPTable(colCount);
        tableVal.setWidthPercentage(100);

        // Título ocupa todas as colunas
        PdfPCell tituloCell = new PdfPCell(new Phrase("Valores", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setBackgroundColor(new Color(245, 245, 245));
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setPadding(6f);
        tituloCell.setColspan(colCount);
        tableVal.addCell(tituloCell);

        // Cabeçalho
        tableVal.addCell(getHeaderCell("Passageiro"));
        tableVal.addCell(getHeaderCell("Tarifa"));
        if (temEmbarque) {
            tableVal.addCell(getHeaderCell("Taxa Embarque"));
        }
        if (temAssento) {
            tableVal.addCell(getHeaderCell("Taxa Assento"));
        }
        if (temDU) {
            tableVal.addCell(getHeaderCell("Taxa DU"));
        }
        if (temRAV) {
            tableVal.addCell(getHeaderCell("RAV"));
        }
        if (temRC) {
            tableVal.addCell(getHeaderCell("RC"));
        }
        if (temTaxaCombustivel) {
            tableVal.addCell(getHeaderCell("Taxa Combustível"));
        }
        tableVal.addCell(getHeaderCell("Total"));

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        // Linhas
        for (PassageiroModel p2 : reserva.getPassageiros()) {
            tableVal.addCell(getCell(p2.getFaixaEtaria() + " - " + p2.getSobrenome() + "/" + p2.getNome(), smallFont));
            for (ReservaValoresAereo v : p2.getValores()) {
                tableVal.addCell(getCell(currencyFormat.format(v.getValorTarifa()), smallFont));
                if (temEmbarque) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getValorTaxaEmbarque()), smallFont));
                }
                if (temAssento) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getTaxaAssento()), smallFont));
                }
                if (temDU) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getTaxaDu()), smallFont));
                }
                if (temRAV) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getTaxaRav()), smallFont));
                }
                if (temRC) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getTaxaRc()), smallFont));
                }
                if (temTaxaCombustivel) {
                    tableVal.addCell(getCell(currencyFormat.format(v.getValorTxCombustivel()), smallFont));
                }

                double total = v.getValorTarifa()
                        + (v.getValorTaxaEmbarque() != null ? v.getValorTaxaEmbarque() : 0)
                        + (v.getTaxaAssento() != null ? v.getTaxaAssento() : 0)
                        + (v.getTaxaDu() != null ? v.getTaxaDu() : 0)
                        + (v.getTaxaRav() != null ? v.getTaxaRav() : 0)
                        + (v.getTaxaRc() != null ? v.getTaxaRc() : 0)
                        + (v.getValorTxCombustivel() != null ? v.getValorTxCombustivel() : 0);

                tableVal.addCell(getCell(currencyFormat.format(total), smallFont));
            }
        }

        // Footer com totais
        tableVal.addCell(getCell("TOTAL", boldFont));
        tableVal.addCell(getCell(currencyFormat.format(reserva.getTarifaGeral()), boldFont));
        if (temEmbarque) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaEmbarqueGeral()), boldFont));
        }
        if (temAssento) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaAssento()), boldFont));
        }
        if (temDU) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaDUGeral()), boldFont));
        }
        if (temRAV) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaRAVGeral()), boldFont));
        }
        if (temRC) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaRCGeral()), boldFont));
        }
        if (temTaxaCombustivel) {
            tableVal.addCell(getCell(currencyFormat.format(reserva.getTaxaTxCombustivelGeral()), boldFont));
        }
        tableVal.addCell(getCell(currencyFormat.format(reserva.getValorTotalReserva()), boldFont));

        return tableVal;
    }

    private PdfPTable criarTabelaBilhetes(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) {

        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);
        // Título acima da tabela
        PdfPCell tituloCell = new PdfPCell(new Phrase("Bilhetes", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setBackgroundColor(new Color(245, 245, 245)); // cor do fundo (#a1b1c5 do seu XHTML)
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setPadding(6f);
        tituloCell.setColspan(5); // ocupa todas as colunas da tabela

        // Cabeçalho da tabela (5 colunas)
        PdfPTable table = new PdfPTable(new float[]{3, 1, 2, 1, 2});
        table.setWidthPercentage(100);

        // Adiciona título como uma linha separada acima
        table.addCell(tituloCell);

        // Cabeçalhos
        table.addCell(getHeaderCell("Passageiro"));
        table.addCell(getHeaderCell("Status"));
        table.addCell(getHeaderCell("Número do Bilhete"));
        table.addCell(getHeaderCell("Data de Emissão"));
        table.addCell(getHeaderCell("Data Cancelamento"));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Itera passageiros e bilhetes
        for (PassageiroModel passageiro : reserva.getPassageiros()) {
            for (BilheteModel bilhete : passageiro.getBilhetes()) {

                // Adiciona +4 horas em Partida e Chegada
                Calendar cal = Calendar.getInstance();

                cal.setTime(bilhete.getDataEmissao());
                cal.add(Calendar.HOUR_OF_DAY, 4);
                String dataEmissaoAjustada = sdf.format(cal.getTime());

                // Passageiro
                table.addCell(getCell(passageiro.getNome() + " / " + passageiro.getSobrenome(), smallFont));

                // Status
                /* String statusTxt = "";
                if ("1".equals(bilhete.getStatus())) {
                    statusTxt = "Emitido";
                } else if ("3".equals(bilhete.getStatus())) {
                    statusTxt = "Cancelado";
                }*/
                String statusTxt = "";
                try {
                    int statusInt = bilhete.getStatus();
                    if (statusInt == 1) {
                        statusTxt = "Emitido";
                    } else if (statusInt == 3) {
                        statusTxt = "Cancelado";
                    }
                } catch (NumberFormatException e) {
                    statusTxt = "-"; // fallback se vier valor inválido
                }

                table.addCell(getCell(statusTxt, smallFont));

                //table.addCell(getCell(bilhete.getStatus().toString(), normalFont));
                // Número do Bilhete
                table.addCell(getCell(bilhete.getNumeroBilhete(), smallFont));

                // Data de Emissão
                table.addCell(getCell(dataEmissaoAjustada, smallFont));

                // Data de Cancelamento
                table.addCell(getCell(formatarData(bilhete.getDataCancelamento(), "dd/MM/yyyy HH:mm"), smallFont));
            }
        }

        return table;
    }

    private PdfPTable criarTabelaRecebimentos(ReservaAereoModelPDF reserva, Font normalFont, Font headerFont) throws DocumentException {

        // Se não tiver recebimentos, retorna null
        if (reserva.getRecebimentos() == null || reserva.getRecebimentos().isEmpty()) {
            return null;
        }

        // Fonte menor para o conteúdo da tabela
        Font smallFont = new Font(Font.HELVETICA, 6f, Font.NORMAL, Color.BLACK);

        // Título acima da tabela
        PdfPCell tituloCell = new PdfPCell(new Phrase("Pagamentos", new Font(Font.HELVETICA, 8, Font.BOLD)));
        tituloCell.setBackgroundColor(new Color(245, 245, 245)); // cor do fundo (#a1b1c5 do seu XHTML)
        tituloCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tituloCell.setPadding(6f);
        tituloCell.setColspan(5); // ocupa todas as colunas da tabela

        // Cabeçalho da tabela (5 colunas)
        PdfPTable tableRece = new PdfPTable(5);
        tableRece.setWidthPercentage(100);

        // Adiciona título como uma linha separada acima
        tableRece.addCell(tituloCell);

        // Cabeçalho
        tableRece.addCell(getHeaderCell("Forma de Pagamento"));
        tableRece.addCell(getHeaderCell("Status"));
        tableRece.addCell(getHeaderCell("Pago em"));
        tableRece.addCell(getHeaderCell("Valor Entrada"));
        tableRece.addCell(getHeaderCell("Valor"));

        // Linhas
        for (RecebimentoModel recebimento : reserva.getRecebimentos()) {
            tableRece.addCell(getCell(recebimento.getNomeFormaPagamento(), smallFont));

            String statusTxt = "";
            try {
                int statusInt = recebimento.getStatusRecebimento();
                if (statusInt == 1) {
                    statusTxt = "Emitido";
                } else if (statusInt == 3) {
                    statusTxt = "Cancelado";
                }
            } catch (NumberFormatException e) {
                statusTxt = "-"; // fallback se vier valor inválido
            }

            tableRece.addCell(getCell(statusTxt, smallFont));

            String dataReceb = recebimento.getDataRecebimento() != null
                    ? new SimpleDateFormat("dd/MM/yyyy").format(recebimento.getDataRecebimento())
                    : "";
            tableRece.addCell(getCell(dataReceb, smallFont));

            //tableRece.addCell(getCell(currencyFormat.format(recebimento.getValorEntrada()), normalFont));
            tableRece.addCell(getCell(
                    recebimento.getValorEntrada() != null
                            ? String.format("R$ %.2f", recebimento.getValorEntrada())
                            : "-", smallFont));
            //tableRece.addCell(getCell(currencyFormat.format(recebimento.getValorPagamento()), normalFont));
            tableRece.addCell(getCell(
                    recebimento.getValorPagamento() != null
                            ? String.format("R$ %.2f", recebimento.getValorPagamento())
                            : "-", smallFont));
        }

        return tableRece;
    }

    // Utilitário para formatar datas
    private static String formatarData(Date data, String pattern) {
        if (data == null) {
            return "";
        }
        return new SimpleDateFormat(pattern).format(data);
    }

    private static PdfPCell getCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);   // centraliza texto
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);     // alinha no meio vertical
        cell.setBackgroundColor(Color.WHITE);                // fundo branco
        cell.setBorderWidth(0.5f);                           // borda mais fina
        return cell;
    }

    private static PdfPCell getHeaderCell(String text) {
        Font headerFont = new Font(Font.HELVETICA, 7, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(230, 230, 230)); // fundo cinza claro
        cell.setPadding(5f);
        return cell;
    }

}
