package com.mindjournal.service.embedding;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.exception.EmbeddingException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Component
@Profile("test")
public class MemoryEmbeddingService implements EmbeddingService {

    private final RagIngestionProperties properties;

    public MemoryEmbeddingService(RagIngestionProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto não pode ser vazio.");
        }

        int dimension = properties.getEmbedding().getDimension();
        float[] embedding = new float[dimension];

        long seed = deriveSeed(text);
        Random random = new Random(seed);

        for (int i = 0; i < dimension; i++) {
            embedding[i] = random.nextFloat();
        }

        return embedding;
    }

    private long deriveSeed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash, 0, 8).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new EmbeddingException("SHA-256 não está disponível.", e);
        }
    }
}
