package com.mindjournal.service.parsing;

import com.mindjournal.exception.EmptyDocumentException;
import com.mindjournal.exception.ParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SimpleDocumentParser implements DocumentParser {

    @Override
    public String parse(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new ParsingException("O conteúdo do arquivo está vazio.");
        }

        if (contentType == null) {
            throw new ParsingException("O tipo do arquivo não foi informado.");
        }

        String text = switch (contentType) {
            case "text/plain" -> parseTxt(content);
            case "application/pdf" -> parsePdf(content);
            default -> throw new ParsingException(
                "Tipo de arquivo não suportado: " + contentType
            );
        };

        if (text.isBlank()) {
            throw new EmptyDocumentException(
                "O documento não possui conteúdo textual após a extração."
            );
        }

        return text;
    }

    private String parseTxt(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private String parsePdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ParsingException(
                "Erro ao processar arquivo PDF: " + e.getMessage(), e
            );
        }
    }
}
