package com.mindjournal.service.chunking;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.config.RagIngestionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class FixedSizeTextChunkerTest {

    private RagIngestionProperties validProps;
    private FixedSizeTextChunker chunker;

    @BeforeEach
    void setUp() {
        validProps = new RagIngestionProperties();
        validProps.getChunk().setMaxSize(10);
        validProps.getChunk().setOverlap(2);
        validProps.getEmbedding().setDimension(768);
        chunker = new FixedSizeTextChunker(validProps);
    }

    @Test
    @DisplayName("texto menor que maxSize retorna um único chunk")
    void textShorterThanMaxSize() {
        String text = "abc";
        List<String> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    @DisplayName("texto exatamente igual a maxSize retorna um chunk")
    void textEqualToMaxSize() {
        String text = "abcdefghij";
        List<String> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    @DisplayName("texto maior que maxSize produz múltiplos chunks")
    void multipleChunks() {
        String text = "0123456789ABCDEFGHIJ";
        // maxSize=10, overlap=2, step=8
        // chunk[0]: [0..10) = "0123456789"
        // chunk[1]: [8..18) = "89ABCDEFGH"
        // chunk[2]: [16..20) = "GHIJ"
        List<String> chunks = chunker.chunk(text);
        assertEquals(3, chunks.size());
        assertEquals("0123456789", chunks.get(0));
        assertEquals("89ABCDEFGH", chunks.get(1));
        assertEquals("GHIJ", chunks.get(2));
    }

    @Test
    @DisplayName("overlap preserva caracteres entre chunks adjacentes")
    void overlapPreservesCharacters() {
        validProps.getChunk().setMaxSize(6);
        validProps.getChunk().setOverlap(3);
        chunker = new FixedSizeTextChunker(validProps);

        String text = "123456789abc";
        // maxSize=6, overlap=3, step=3
        // chunk[0]: [0..6) = "123456"
        // chunk[1]: [3..9) = "456789"
        // chunk[2]: [6..12) = "789abc"
        List<String> chunks = chunker.chunk(text);

        assertTrue(chunks.size() >= 2);
        String chunk0End = chunks.get(0).substring(chunks.get(0).length() - 3);
        String chunk1Start = chunks.get(1).substring(0, 3);
        assertEquals(chunk0End, chunk1Start);
    }

    @Test
    @DisplayName("chamadas repetidas produzem a mesma ordem")
    void deterministicOrder() {
        String text = "abcdefghijklmnopqrstuvwxyz";
        List<String> first = chunker.chunk(text);
        List<String> second = chunker.chunk(text);
        assertEquals(first, second);
    }

    @Test
    @DisplayName("texto nulo lança IllegalArgumentException")
    void nullText() {
        assertThrows(IllegalArgumentException.class,
            () -> chunker.chunk(null));
    }

    @Test
    @DisplayName("texto vazio lança IllegalArgumentException")
    void emptyText() {
        assertThrows(IllegalArgumentException.class,
            () -> chunker.chunk(""));
    }

    @Test
    @DisplayName("texto somente whitespace lança IllegalArgumentException")
    void blankText() {
        assertThrows(IllegalArgumentException.class,
            () -> chunker.chunk("   "));
    }

    @Test
    @DisplayName("configuração inválida no chunker lança IllegalStateException")
    void invalidConfiguration() {
        var invalidProps = new RagIngestionProperties();
        invalidProps.getChunk().setMaxSize(5);
        invalidProps.getChunk().setOverlap(5);
        invalidProps.getEmbedding().setDimension(768);

        var invalidChunker = new FixedSizeTextChunker(invalidProps);

        assertThrows(IllegalStateException.class,
            () -> invalidChunker.chunk("qualquer texto"));
    }

    @Test
    @DisplayName("texto longo exato sem overflow no loop")
    void exactMultiple() {
        validProps.getChunk().setMaxSize(5);
        validProps.getChunk().setOverlap(0);
        chunker = new FixedSizeTextChunker(validProps);

        String text = "1234567890";
        List<String> chunks = chunker.chunk(text);
        assertEquals(2, chunks.size());
        assertEquals("12345", chunks.get(0));
        assertEquals("67890", chunks.get(1));
    }
}
