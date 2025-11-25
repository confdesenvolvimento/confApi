package com.confApi.geradorPdf;

import com.confApi.geradorPdf.aereo.ReservaAereoModelPDF;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class GeradorService {

    // Caminho do arquivo HTML na pasta resources
    private static final String HTML_FILE_PATH = "templates/reservaAereo.html";

    // Remova o construtor do TemplateEngine
    // public SeuServicoDePDF(TemplateEngine htmlTemplateEngine) { ... }

    // Método principal
   /* public byte[] gerarReservaAereoPDFBytes(ReservaAereoModelPDF aereoPDF)
            throws IOException {

        // 1. LER O CONTEÚDO BRUTO DO ARQUIVO HTML
        String rawHtmlTemplate = loadHtmlTemplate(HTML_FILE_PATH);

        // 2. INJETAR DADOS DINÂMICOS NO HTML (Substituindo o papel do Thymeleaf)
        String htmlContent = injectDynamicData(rawHtmlTemplate, aereoPDF);

        // 3. Converter o HTML gerado para bytes de PDF
        byte[] pdfBytes = convertHtmlToPdfBytes(htmlContent);

        return pdfBytes;
    }*/


    /**
     * Carrega o arquivo HTML do classpath (pasta resources).
     */
    private String loadHtmlTemplate(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            // Usa o utilitário do Spring para copiar o conteúdo do Reader para uma String
            return FileCopyUtils.copyToString(reader);
        }
    }

    /**
     * Injeta dados do objeto aereoPDF no HTML.
     * ATENÇÃO: Essa é a parte mais complexa e que requer cuidado com sintaxe HTML.
     */
    private String injectDynamicData(String rawHtml, ReservaAereoModelPDF aereoPDF) {

        // Exemplo simples: você precisará de um sistema de marcadores (placeholders) no seu HTML

        // 1. Defina os dados
        String localizador = aereoPDF.getLocalizador() != null ? aereoPDF.getLocalizador() : "-";
        String status = aereoPDF.getStatusReserva() != null ? aereoPDF.getStatusReserva() : "-";
        // Dados para os logos (Fixos ou vindo de aereoPDF)
        String urlAgencia = aereoPDF.getAgencia().getLogomarca(); // Fixado
        String urlCia = "https://cdn.westonline.tur.br/confianca.tur/img/cia_short/"+aereoPDF.getCodgCompanhiaAerea().getIataCia()+".png"; //
        // Dados do usuário (Assumindo que estão em aereoPDF)
        String nomeUsuario = aereoPDF.getUsuarioCriacao().getNomeCompleto();
        String emailUsuario = aereoPDF.getUsuarioCriacao().getEmail();
        String telefoneUsuario = aereoPDF.getUsuarioCriacao().getTelefone();// Fixado ou aereoPDF.getCiaLogoUrl()

        // 2. Substitua os marcadores no HTML
        String finalHtml = rawHtml
                .replace("{{LOCALIZADOR}}", localizador)
                .replace("{{STATUS_RESERVA}}", status)
                // ... adicione mais substituições aqui
                .replace("{{NOME_AGENCIA}}", aereoPDF.getAgencia().getNomeAgencia())
        .replace("{{URL_LOGO_AGENCIA}}", urlAgencia)
                .replace("{{URL_LOGO_CIA}}", urlCia)
                .replace("{{USUARIO_NOME}}", nomeUsuario)
                .replace("{{USUARIO_EMAIL}}", emailUsuario)
                .replace("{{USUARIO_TELEFONE}}", telefoneUsuario);

        return finalHtml;
    }


    // O método convertHtmlToPdfBytes permanece o mesmo
    private byte[] convertHtmlToPdfBytes(String htmlContent) throws IOException {
        // ... (seu código de conversão aqui)
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IOException("Erro ao converter HTML para PDF: " + e.getMessage(), e);
        }
    }
}



