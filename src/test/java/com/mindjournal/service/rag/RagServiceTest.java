package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.RagRetrievalProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import vector.rag.repository.DocumentChunkRepository;
import vector.rag.repository.RelevantChunkProjection;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private RagRetrievalProperties retrievalProperties;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        retrievalProperties = new RagRetrievalProperties();
        retrievalProperties.setTopK(3);
        retrievalProperties.setMinSimilarity(0.70);
        ragService = new RagService(embeddingService, chunkRepository, retrievalProperties);
    }

    @Test
    @DisplayName("RagService usa topK configurado")
    void usesConfiguredTopK() {
        float[] embedding = new float[768];
        embedding[0] = 0.5f;
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of());

        ragService.retrieveContext(1L, "consulta");

        verify(chunkRepository).findRelevantChunks(eq(1L), eq(embedding), eq(0.70), argThat(p ->
                ((Pageable) p).getPageSize() == 3
        ));
    }

    @Test
    @DisplayName("RagService usa minSimilarity configurado")
    void usesConfiguredMinSimilarity() {
        retrievalProperties.setMinSimilarity(0.50);
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of());

        ragService.retrieveContext(1L, "consulta");

        verify(chunkRepository).findRelevantChunks(eq(1L), eq(embedding), eq(0.50), any());
    }

    @Test
    @DisplayName("RagService passa sessionId ao repository")
    void passesSessionIdToRepository() {
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of());

        ragService.retrieveContext(42L, "consulta");

        verify(chunkRepository).findRelevantChunks(eq(42L), any(), anyDouble(), any());
    }

    @Test
    @DisplayName("RagService preserva ordem dos resultados")
    void preservesResultOrder() {
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);

        var result1 = new RelevantChunkProjection(1L, "a.txt", 10L, "texto1", 0.95);
        var result2 = new RelevantChunkProjection(1L, "a.txt", 11L, "texto2", 0.80);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of(result1, result2));

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertEquals(2, ctx.sources().size());
        assertEquals(0.95, ctx.sources().get(0).similarityScore());
        assertEquals(0.80, ctx.sources().get(1).similarityScore());
    }

    @Test
    @DisplayName("RagService retorna score real do repository")
    void returnsRealScore() {
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);

        var result = new RelevantChunkProjection(1L, "a.txt", 10L, "texto", 0.8723);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of(result));

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertEquals(0.8723, ctx.sources().get(0).similarityScore(), 0.0001);
    }

    @Test
    @DisplayName("RagService retorna lista vazia quando não há chunks")
    void returnsEmptyWhenNoChunks() {
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of());

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertTrue(ctx.sources().isEmpty());
        assertTrue(ctx.context().isEmpty());
    }

    @Test
    @DisplayName("RagService propaga falha do EmbeddingService")
    void propagatesEmbeddingFailure() {
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new RuntimeException("Falha no Ollama"));

        assertThrows(RuntimeException.class,
            () -> ragService.retrieveContext(1L, "consulta"));
    }

    @Test
    @DisplayName("RagService mapeia campos corretamente")
    void mapsFieldsCorrectly() {
        float[] embedding = new float[768];
        when(embeddingService.generateEmbedding("consulta")).thenReturn(embedding);

        var result = new RelevantChunkProjection(5L, "teste-rag.txt", 10L, "Trecho utilizado", 0.87);
        when(chunkRepository.findRelevantChunks(anyLong(), any(), anyDouble(), any()))
                .thenReturn(List.of(result));

        RagContext ctx = ragService.retrieveContext(1L, "consulta");
        SourceDTO dto = ctx.sources().get(0);

        assertEquals(5L, dto.documentId());
        assertEquals("teste-rag.txt", dto.fileName());
        assertEquals(10L, dto.chunkId());
        assertEquals("Trecho utilizado", dto.content());
        assertEquals(0.87, dto.similarityScore(), 0.0001);
    }
}
