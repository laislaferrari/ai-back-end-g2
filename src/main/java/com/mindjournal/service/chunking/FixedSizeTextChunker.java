package com.mindjournal.service.chunking;

import com.mindjournal.config.RagIngestionProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeTextChunker implements TextChunker {

    private final RagIngestionProperties properties;

    public FixedSizeTextChunker(RagIngestionProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto não pode ser vazio.");
        }

        int maxSize = properties.getChunk().getMaxSize();
        int overlap = properties.getChunk().getOverlap();

        if (maxSize <= 0 || overlap < 0 || overlap >= maxSize) {
            throw new IllegalStateException(
                "Configuração de chunking inválida: maxSize=" + maxSize + ", overlap=" + overlap
            );
        }

        int step = maxSize - overlap;
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            String fragment = text.substring(start, end);
            chunks.add(fragment);

            if (end == text.length()) {
                break;
            }

            start += step;

            if (start >= text.length()) {
                break;
            }
        }

        return chunks;
    }
}
