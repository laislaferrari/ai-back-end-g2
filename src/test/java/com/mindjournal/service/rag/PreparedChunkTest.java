package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class PreparedChunkTest {

    private static final int DIM = 768;

    private float[] validEmbedding() {
        float[] e = new float[DIM];
        Arrays.fill(e, 0.5f);
        return e;
    }

    @Test
    @DisplayName("dimensão válida cria o record")
    void validDimension() {
        float[] emb = validEmbedding();
        PreparedChunk chunk = new PreparedChunk("conteúdo", 0, emb);
        assertEquals("conteúdo", chunk.content());
        assertEquals(0, chunk.chunkIndex());
        assertEquals(DIM, chunk.embedding().length);
    }

    @Test
    @DisplayName("dimensão inválida lança IllegalArgumentException")
    void invalidDimension() {
        float[] emb = new float[767];
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk("conteúdo", 0, emb));
    }

    @Test
    @DisplayName("cópia defensiva no construtor isola o record")
    void defensiveCopyInConstructor() {
        float[] emb = validEmbedding();
        PreparedChunk chunk = new PreparedChunk("conteúdo", 0, emb);
        emb[0] = 999f;
        assertNotEquals(999f, chunk.embedding()[0], 0.0001f);
    }

    @Test
    @DisplayName("cópia defensiva no accessor isola o array retornado")
    void defensiveCopyInAccessor() {
        float[] emb = validEmbedding();
        PreparedChunk chunk = new PreparedChunk("conteúdo", 0, emb);
        float[] returned = chunk.embedding();
        returned[0] = 999f;
        assertNotEquals(999f, chunk.embedding()[0], 0.0001f);
    }

    @Test
    @DisplayName("conteúdo vazio lança IllegalArgumentException")
    void emptyContent() {
        float[] emb = validEmbedding();
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk("", 0, emb));
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk("   ", 0, emb));
    }

    @Test
    @DisplayName("conteúdo nulo lança IllegalArgumentException")
    void nullContent() {
        float[] emb = validEmbedding();
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk(null, 0, emb));
    }

    @Test
    @DisplayName("índice negativo lança IllegalArgumentException")
    void negativeIndex() {
        float[] emb = validEmbedding();
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk("conteúdo", -1, emb));
    }

    @Test
    @DisplayName("embedding nulo lança IllegalArgumentException")
    void nullEmbedding() {
        assertThrows(IllegalArgumentException.class,
            () -> new PreparedChunk("conteúdo", 0, null));
    }
}
