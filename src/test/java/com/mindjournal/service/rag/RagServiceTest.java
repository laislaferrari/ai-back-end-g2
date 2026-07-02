package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.RagRetrievalProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentChunk;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.repository.DocumentChunkRepository;
import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private RagRetrievalProperties retrievalProperties;
    private RagService ragService;

    private Document document;
    private DocumentChunk chunk1;
    private DocumentChunk chunk2;

    @BeforeEach
    void setUp() {
        retrievalProperties = new RagRetrievalProperties();
        retrievalProperties.setTopK(3);
        retrievalProperties.setMinSimilarity(0.50);
        ragService = new RagService(embeddingService, chunkRepository, retrievalProperties);

        Attachment attachment = new Attachment();
        attachment.setId(1L);
        attachment.setFilename("doc.pdf");
        attachment.setType(AttachmentType.PDF);

        document = new Document(attachment);
        document.markAsProcessing();
        document.markAsIndexed();

        chunk1 = new DocumentChunk(document, "conteúdo relevante sobre IA", 0, randomEmbedding());
        chunk2 = new DocumentChunk(document, "outro texto qualquer", 1, randomEmbedding());
    }

    @Test
    @DisplayName("retorna chunks relevantes com sources corretos")
    void returnsRelevantChunks() {
        when(embeddingService.generateEmbedding("consulta")).thenReturn(randomEmbedding());
        when(chunkRepository.findAllBySessionId(1L)).thenReturn(List.of(chunk1, chunk2));

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertNotNull(ctx);
        assertFalse(ctx.sources().isEmpty());
    }

    @Test
    @DisplayName("retorna sources vazia quando não há chunks")
    void returnsEmptyWhenNoChunks() {
        when(embeddingService.generateEmbedding("consulta")).thenReturn(randomEmbedding());
        when(chunkRepository.findAllBySessionId(1L)).thenReturn(List.of());

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertTrue(ctx.sources().isEmpty());
        assertTrue(ctx.context().isEmpty());
    }

    @Test
    @DisplayName("propaga falha do EmbeddingService")
    void propagatesEmbeddingFailure() {
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new RuntimeException("Falha no Ollama"));

        assertThrows(RuntimeException.class,
            () -> ragService.retrieveContext(1L, "consulta"));
    }

    @Test
    @DisplayName("retorna lista vazia quando minSimilarity é alto demais")
    void returnsEmptyWithHighThreshold() {
        retrievalProperties.setMinSimilarity(0.99);
        float[] queryEmb = new float[768];
        queryEmb[0] = 1.0f;

        when(embeddingService.generateEmbedding("consulta")).thenReturn(queryEmb);

        DocumentChunk farChunk = new DocumentChunk(document, "muito diferente", 0, new float[768]);
        when(chunkRepository.findAllBySessionId(1L)).thenReturn(List.of(farChunk));

        RagContext ctx = ragService.retrieveContext(1L, "consulta");

        assertTrue(ctx.sources().isEmpty());
    }

    private static float[] randomEmbedding() {
        float[] emb = new float[768];
        emb[0] = 0.5f;
        emb[1] = 0.3f;
        return emb;
    }
}
