package com.mindjournal.service.embedding;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.exception.EmbeddingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Profile("!test")
public class OllamaEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final RagIngestionProperties properties;

    @Autowired
    public OllamaEmbeddingService(RagIngestionProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    OllamaEmbeddingService(RagIngestionProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] generateEmbedding(String text) {
        Map<String, Object> request = Map.of(
            "model", properties.getOllamaModel(),
            "input", text
        );

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(properties.getOllamaUrl(), request, Map.class);
        } catch (Exception e) {
            throw new EmbeddingException("Falha de conexão com o serviço de embeddings.", e);
        }

        if (response.getBody() == null || !response.getBody().containsKey("embeddings")) {
            throw new EmbeddingException("Resposta do serviço de embeddings não contém o campo 'embeddings'.");
        }

        List<List<Double>> embeddings = (List<List<Double>>) response.getBody().get("embeddings");
        if (embeddings == null || embeddings.isEmpty()) {
            throw new EmbeddingException("Serviço de embeddings retornou uma lista vazia.");
        }

        List<Double> vector = embeddings.get(0);
        int expectedDimension = properties.getEmbedding().getDimension();
        if (vector.size() != expectedDimension) {
            throw new EmbeddingException(
                "Dimensão do embedding retornada (" + vector.size()
                    + ") difere da esperada (" + expectedDimension + ")."
            );
        }

        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }
}