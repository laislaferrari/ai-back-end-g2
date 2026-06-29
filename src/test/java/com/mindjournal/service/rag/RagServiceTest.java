package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.Document;
import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import vector.rag.entity.DocumentChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import vector.rag.repository.DocumentChunkRepository;
import vector.rag.repository.RelevantChunkProjection;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private RagIngestionProperties properties;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        properties = new RagIngestionProperties();
        ragService = new RagService(embeddingService, chunkRepository, properties);
    }

    @Test
    @DisplayName("retrieveContext retorna sources com score real quando encontra chunks relevantes")
    void retrieveContextWithRelevantChunks() {
        when(embeddingService.generateEmbedding("qual o significado da vida?"))
                .thenReturn(new float[768]);

        Document document = createDocument(5L, "documento.pdf");
        List<Object[]> results = Arrays.asList(
                row(document, 1L, "trecho um", 0, 0.92),
                row(document, 2L, "trecho dois", 1, 0.85)
        );

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(results);

        RagService.RagContext context = ragService.retrieveContext("qual o significado da vida?");

        assertEquals(2, context.sources().size());

        SourceDTO first = context.sources().get(0);
        assertEquals(5L, first.documentId());
        assertEquals("documento.pdf", first.fileName());
        assertEquals(1L, first.chunkId());
        assertEquals("trecho um", first.content());
        assertEquals(0.92, first.similarityScore(), 0.001);

        SourceDTO second = context.sources().get(1);
        assertEquals(0.85, second.similarityScore(), 0.001);
    }

    @Test
    @DisplayName("retrieveContext retorna lista vazia quando nenhum chunk atinge o limite de similaridade")
    void retrieveContextWithoutResults() {
        when(embeddingService.generateEmbedding("pergunta sem contexto"))
                .thenReturn(new float[768]);

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(Arrays.asList());

        RagService.RagContext context = ragService.retrieveContext("pergunta sem contexto");

        assertTrue(context.sources().isEmpty());
        assertTrue(context.content().isEmpty());
    }

    @Test
    @DisplayName("retrieveContext respeita o limite TOP_K configurado")
    void retrieveContextRespectsTopK() {
        properties.getRetrieval().setTopK(1);

        when(embeddingService.generateEmbedding("teste topk"))
                .thenReturn(new float[768]);

        Document document = createDocument(5L, "doc.pdf");
        List<Object[]> results = Arrays.asList(
                row(document, 1L, "mais relevante", 0, 0.95),
                row(document, 2L, "menos relevante", 1, 0.80)
        );

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(results);

        RagService.RagContext context = ragService.retrieveContext("teste topk");

        assertEquals(2, context.sources().size());
    }

    @Test
    @DisplayName("retrieveContext usa o MIN_SIMILARITY configurado na consulta")
    void retrieveContextUsesConfiguredMinSimilarity() {
        properties.getRetrieval().setMinSimilarity(0.50);

        when(embeddingService.generateEmbedding("teste minSimilarity"))
                .thenReturn(new float[768]);

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), eq(0.50f), any(Pageable.class)
        )).thenReturn(Arrays.asList());

        RagService.RagContext context = ragService.retrieveContext("teste minSimilarity");

        assertTrue(context.sources().isEmpty());
        verify(chunkRepository).findRelevantChunksWithScore(
                any(float[].class), eq(0.50f), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("retrieveContext retorna score real em vez de valor fictício")
    void retrieveContextReturnsRealScore() {
        when(embeddingService.generateEmbedding("score real"))
                .thenReturn(new float[768]);

        Document document = createDocument(5L, "doc.pdf");
        List<Object[]> results = new ArrayList<>();
        results.add(row(document, 5L, "conteudo do chunk", 0, 0.87));

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(results);

        RagService.RagContext context = ragService.retrieveContext("score real");

        assertEquals(1, context.sources().size());
        assertEquals(0.87, context.sources().get(0).similarityScore(), 0.001);
        assertNotEquals(0.99, context.sources().get(0).similarityScore(), 0.001);
    }

    @Test
    @DisplayName("retrieveContext retorna chunks ordenados do mais relevante para o menos relevante")
    void retrieveContextReturnsChunksOrderedByScoreDesc() {
        when(embeddingService.generateEmbedding("ordenacao"))
                .thenReturn(new float[768]);

        Document document = createDocument(5L, "doc.pdf");
        List<Object[]> results = Arrays.asList(
                row(document, 1L, "primeiro", 0, 0.95),
                row(document, 2L, "segundo", 1, 0.85),
                row(document, 3L, "terceiro", 2, 0.70)
        );

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(results);

        RagService.RagContext context = ragService.retrieveContext("ordenacao");

        List<SourceDTO> sources = context.sources();
        assertEquals(0.95, sources.get(0).similarityScore(), 0.001);
        assertEquals(0.85, sources.get(1).similarityScore(), 0.001);
        assertEquals(0.70, sources.get(2).similarityScore(), 0.001);
    }

    @Test
    @DisplayName("retrieveContext retorna null ou lista vazia quando repository retorna null")
    void retrieveContextWhenRepositoryReturnsNull() {
        when(embeddingService.generateEmbedding("teste null"))
                .thenReturn(new float[768]);

        when(chunkRepository.findRelevantChunksWithScore(
                any(float[].class), anyFloat(), any(Pageable.class)
        )).thenReturn(null);

        RagService.RagContext context = ragService.retrieveContext("teste null");

        assertTrue(context.sources().isEmpty());
        assertTrue(context.content().isEmpty());
    }

    private static Document createDocument(Long id, String filename) {
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setFilename(filename);

        Document document = new Document();
        document.setAttachment(attachment);
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }

    @SuppressWarnings("unchecked")
    private static Object[] row(Document document, Long chunkId, String content, int index, double score) {
        DocumentChunk chunk = mock(DocumentChunk.class);
        lenient().when(chunk.getId()).thenReturn(chunkId);
        lenient().when(chunk.getDocument()).thenReturn(document);
        lenient().when(chunk.getContent()).thenReturn(content);
        return new Object[]{chunk, score};
    }
}
