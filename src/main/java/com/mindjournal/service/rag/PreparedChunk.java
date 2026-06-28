package com.mindjournal.service.rag;

import java.util.Arrays;

public record PreparedChunk(
    String content,
    int chunkIndex,
    float[] embedding
) {

    private static final int EXPECTED_DIMENSION = 768;

    public PreparedChunk(String content, int chunkIndex, float[] embedding) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("O conteúdo do chunk não pode ser vazio.");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("O índice do chunk não pode ser negativo.");
        }
        if (embedding == null) {
            throw new IllegalArgumentException("O embedding não pode ser nulo.");
        }
        if (embedding.length != EXPECTED_DIMENSION) {
            throw new IllegalArgumentException(
                "O embedding deve possuir exatamente " + EXPECTED_DIMENSION + " posições."
            );
        }
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.embedding = Arrays.copyOf(embedding, embedding.length);
    }

    @Override
    public float[] embedding() {
        return Arrays.copyOf(embedding, embedding.length);
    }
}
