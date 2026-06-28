package com.mindjournal.service.embedding;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.config.RagIngestionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class MemoryEmbeddingServiceTest {

    private RagIngestionProperties props;
    private MemoryEmbeddingService service;

    @BeforeEach
    void setUp() {
        props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(10);
        props.getEmbedding().setDimension(768);
        service = new MemoryEmbeddingService(props);
    }

    @Test
    @DisplayName("vetor retornado possui exatamente 768 posições")
    void dimensionIs768() {
        float[] embedding = service.generateEmbedding("qualquer texto");
        assertEquals(768, embedding.length);
    }

    @Test
    @DisplayName("mesma entrada produz o mesmo vetor")
    void deterministicForSameInput() {
        float[] first = service.generateEmbedding("texto determinístico");
        float[] second = service.generateEmbedding("texto determinístico");
        assertArrayEquals(first, second, 0.0001f);
    }

    @Test
    @DisplayName("entradas diferentes produzem vetores diferentes")
    void differentInputsProduceDifferentVectors() {
        float[] first = service.generateEmbedding("primeiro texto");
        float[] second = service.generateEmbedding("segundo texto");
        assertFalse(Arrays.equals(first, second));
    }

    @Test
    @DisplayName("FB e Ea produzem vetores diferentes mesmo com mesmo hashCode")
    void fbAndEaProduceDifferentVectors() {
        assertEquals("FB".hashCode(), "Ea".hashCode());
        float[] fb = service.generateEmbedding("FB");
        float[] ea = service.generateEmbedding("Ea");
        assertFalse(Arrays.equals(fb, ea));
    }

    @Test
    @DisplayName("texto nulo lança IllegalArgumentException")
    void nullText() {
        assertThrows(IllegalArgumentException.class,
            () -> service.generateEmbedding(null));
    }

    @Test
    @DisplayName("texto vazio lança IllegalArgumentException")
    void emptyText() {
        assertThrows(IllegalArgumentException.class,
            () -> service.generateEmbedding(""));
    }

    @Test
    @DisplayName("texto somente whitespace lança IllegalArgumentException")
    void blankText() {
        assertThrows(IllegalArgumentException.class,
            () -> service.generateEmbedding("   "));
    }

    @Test
    @DisplayName("todos os valores estão entre 0 e 1")
    void valuesInRange() {
        float[] embedding = service.generateEmbedding("range test");
        for (float v : embedding) {
            assertTrue(v >= 0.0f && v < 1.0f,
                "valor " + v + " fora do intervalo [0, 1)");
        }
    }
}
