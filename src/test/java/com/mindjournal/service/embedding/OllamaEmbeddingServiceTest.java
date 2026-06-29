package com.mindjournal.service.embedding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.exception.EmbeddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private RagIngestionProperties properties;
    private OllamaEmbeddingService service;

    private static final int DIM = 768;

    @BeforeEach
    void setUp() {
        properties = new RagIngestionProperties();
        properties.getEmbedding().setDimension(DIM);
        properties.setOllamaUrl("http://localhost:11434/api/embed");
        properties.setOllamaModel("embeddinggemma:300m");
        service = new OllamaEmbeddingService(properties, restTemplate);
    }

    @Test
    @DisplayName("classe implementa EmbeddingService")
    void implementsEmbeddingService() {
        assertInstanceOf(EmbeddingService.class, service);
    }

    @Test
    @DisplayName("resposta válida retorna float[] com 768 posições")
    void validResponseReturnsCorrectDimension() {
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < DIM; i++) {
            vector.add((double) i / DIM);
        }
        Map<String, Object> body = Map.of("embeddings", List.of(vector));

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(body));

        float[] result = service.generateEmbedding("texto de teste");

        assertEquals(DIM, result.length);
        for (int i = 0; i < DIM; i++) {
            assertEquals((float) i / DIM, result[i], 0.0001f);
        }
    }

    @Test
    @DisplayName("falha HTTP lança EmbeddingException")
    void httpFailureThrowsEmbeddingException() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        EmbeddingException ex = assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
        assertTrue(ex.getMessage().contains("Falha de conexão"));
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("resposta sem campo embeddings lança EmbeddingException")
    void responseWithoutEmbeddingsFieldThrowsEmbeddingException() {
        Map<String, Object> body = Map.of("outro", "campo");

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(body));

        assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
    }

    @Test
    @DisplayName("resposta com lista vazia de embeddings lança EmbeddingException")
    void emptyEmbeddingsListThrowsEmbeddingException() {
        Map<String, Object> body = Map.of("embeddings", List.of());

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(body));

        assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
    }

    @Test
    @DisplayName("vetor com dimensão incorreta lança EmbeddingException")
    void wrongDimensionThrowsEmbeddingException() {
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            vector.add(0.5);
        }
        Map<String, Object> body = Map.of("embeddings", List.of(vector));

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(body));

        EmbeddingException ex = assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
        assertTrue(ex.getMessage().contains("42"));
        assertTrue(ex.getMessage().contains("768"));
    }

    @Test
    @DisplayName("response body nulo lança EmbeddingException")
    void nullBodyThrowsEmbeddingException() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
    }

    @Test
    @DisplayName("é excluído apenas no profile test")
    void excludesTestProfile() {
        Profile profile = OllamaEmbeddingService.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertTrue(List.of(profile.value()).contains("!test"),
            "OllamaEmbeddingService deve ser excluído no profile test");
    }

    @Test
    @DisplayName("MemoryEmbeddingService é restrito ao profile test")
    void memoryServiceHasTestProfile() {
        Profile profile = MemoryEmbeddingService.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertTrue(List.of(profile.value()).contains("test"));
    }

    @Test
    @DisplayName("resposta com embeddings null lança EmbeddingException")
    void nullEmbeddingsListThrowsEmbeddingException() {
        Map<String, Object> body = new HashMap<>();
        body.put("embeddings", null);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(body));

        assertThrows(EmbeddingException.class,
            () -> service.generateEmbedding("texto"));
    }
}
