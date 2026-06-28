package com.mindjournal.service.parsing;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.exception.EmptyDocumentException;
import com.mindjournal.exception.ParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class DocumentParserTest {

    private SimpleDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new SimpleDocumentParser();
    }

    @Test
    @DisplayName("text/plain válido retorna o texto extraído")
    void validTxt() {
        String content = "Olá, mundo! Este é um teste.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String result = parser.parse(bytes, "text/plain");
        assertEquals(content, result);
    }

    @Test
    @DisplayName("text/plain vazio lança EmptyDocumentException")
    void emptyTxt() {
        byte[] bytes = "   ".getBytes(StandardCharsets.UTF_8);
        assertThrows(EmptyDocumentException.class,
            () -> parser.parse(bytes, "text/plain"));
    }

    @Test
    @DisplayName("PDF válido retorna o texto extraído")
    void validPdf() throws IOException {
        byte[] pdfBytes = createMinimalPdf("Hello, PDF World!");

        String result = parser.parse(pdfBytes, "application/pdf");

        assertTrue(result.contains("Hello, PDF World!"));
    }

    @Test
    @DisplayName("PDF inválido lança ParsingException")
    void invalidPdf() {
        byte[] invalid = { 0, 1, 2, 3, 4, 5 };
        assertThrows(ParsingException.class,
            () -> parser.parse(invalid, "application/pdf"));
    }

    @Test
    @DisplayName("content type não suportado lança ParsingException")
    void unsupportedContentType() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        assertThrows(ParsingException.class,
            () -> parser.parse(bytes, "application/json"));
    }

    @Test
    @DisplayName("content type nulo lança ParsingException")
    void nullContentType() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        assertThrows(ParsingException.class,
            () -> parser.parse(bytes, null));
    }

    @Test
    @DisplayName("bytes nulos lança ParsingException")
    void nullBytes() {
        assertThrows(ParsingException.class,
            () -> parser.parse(null, "text/plain"));
    }

    @Test
    @DisplayName("bytes vazios lança ParsingException")
    void emptyBytes() {
        assertThrows(ParsingException.class,
            () -> parser.parse(new byte[0], "text/plain"));
    }

    private byte[] createMinimalPdf(String text) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PDDocument document = new PDDocument()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }
}
