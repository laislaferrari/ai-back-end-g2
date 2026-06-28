package com.mindjournal.service.embedding;

import com.mindjournal.config.RagIngestionProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OllamaEmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RagIngestionProperties properties;

    public OllamaEmbeddingService(RagIngestionProperties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> request = Map.of(
                "model", properties.getOllamaModel(),
                "input", text
            );
            
            ResponseEntity<Map> response = restTemplate.postForEntity(properties.getOllamaUrl(), request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("embeddings")) {
                List<List<Double>> embeddings = (List<List<Double>>) response.getBody().get("embeddings");
                if (!embeddings.isEmpty()) {
                    List<Double> vector = embeddings.get(0);
                    float[] result = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        result[i] = vector.get(i).floatValue();
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao conectar com Ollama: " + e.getMessage());
        }
        return new float[768];
    }
}