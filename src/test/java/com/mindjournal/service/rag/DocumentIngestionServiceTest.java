package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.exception.EmbeddingException;
import com.mindjournal.service.chunking.TextChunker;
import com.mindjournal.service.embedding.EmbeddingService;
import com.mindjournal.service.parsing.DocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private IngestionTransactionService transactionService;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private TextChunker textChunker;

    @Mock
    private ObjectProvider<EmbeddingService> embeddingServiceProvider;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RagIngestionProperties properties;

    private RagIngestionProperties.Embedding embeddingConfig;

    private DocumentIngestionService service;

    private static final Long DOC_ID = 1L;
    private static final int DIM = 768;

    @BeforeEach
    void setUp() {
        embeddingConfig = new RagIngestionProperties.Embedding();
        embeddingConfig.setDimension(DIM);
        lenient().when(properties.getEmbedding()).thenReturn(embeddingConfig);

        service = new DocumentIngestionService(
            transactionService, documentParser, textChunker,
            embeddingServiceProvider, properties
        );
    }

    @Test
    @DisplayName("falha em markAsProcessing não chama markAsFailed")
    void failureInMarkAsProcessingDoesNotCallMarkAsFailed() {
        when(transactionService.markAsProcessing(DOC_ID))
            .thenThrow(new RuntimeException("falha na transação"));

        assertThrows(RuntimeException.class, () -> service.ingest(DOC_ID));

        verify(transactionService, never()).markAsFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("arquivo inexistente após PROCESSING chama markAsFailed")
    void missingFileCallsMarkAsFailed() throws Exception {
        IngestionSource source = new IngestionSource(DOC_ID, "/tmp/nao-existe.txt", "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);

        assertThrows(Exception.class, () -> service.ingest(DOC_ID));

        verify(transactionService).markAsFailed(eq(DOC_ID), anyString());
    }

    @Test
    @DisplayName("ausência de EmbeddingService chama markAsFailed")
    void missingEmbeddingServiceCallsMarkAsFailed() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo de teste");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenReturn("texto extraído");
        when(textChunker.chunk(anyString())).thenReturn(List.of("chunk1", "chunk2"));
        when(embeddingServiceProvider.getIfAvailable()).thenReturn(null);

        assertThrows(EmbeddingException.class, () -> service.ingest(DOC_ID));

        verify(transactionService).markAsFailed(eq(DOC_ID), anyString());
    }

    @Test
    @DisplayName("embedding com dimensão inválida chama markAsFailed")
    void invalidEmbeddingDimensionCallsMarkAsFailed() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo de teste");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenReturn("texto extraído");
        when(textChunker.chunk(anyString())).thenReturn(List.of("chunk1"));
        when(embeddingServiceProvider.getIfAvailable()).thenReturn(embeddingService);
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[767]);

        assertThrows(EmbeddingException.class, () -> service.ingest(DOC_ID));

        verify(transactionService).markAsFailed(eq(DOC_ID), anyString());
    }

    @Test
    @DisplayName("fluxo feliz cria índices 0..N-1 e chama replaceChunksAndMarkIndexed")
    void happyPath() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo longo para gerar múltiplos chunks");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenReturn(
            "um dois três quatro cinco seis sete oito nove dez"
        );
        when(textChunker.chunk(anyString())).thenReturn(
            List.of("chunk A", "chunk B", "chunk C")
        );
        when(embeddingServiceProvider.getIfAvailable()).thenReturn(embeddingService);

        float[] mockEmbedding = new float[DIM];
        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);

        service.ingest(DOC_ID);

        verify(transactionService).replaceChunksAndMarkIndexed(eq(DOC_ID), argThat(chunks -> {
            if (chunks.size() != 3) return false;
            assertEquals(0, chunks.get(0).chunkIndex());
            assertEquals(1, chunks.get(1).chunkIndex());
            assertEquals(2, chunks.get(2).chunkIndex());
            assertEquals("chunk A", chunks.get(0).content());
            assertEquals("chunk B", chunks.get(1).content());
            assertEquals("chunk C", chunks.get(2).content());
            return true;
        }));
        verify(transactionService, never()).markAsFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("TextChunker retorna lista vazia: não chama replace, chama markAsFailed com mensagem de chunking")
    void emptyChunkListDoesNotCallReplace() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenReturn("texto extraído");
        when(textChunker.chunk(anyString())).thenReturn(List.of());

        assertThrows(Exception.class, () -> service.ingest(DOC_ID));

        verify(transactionService, never()).replaceChunksAndMarkIndexed(anyLong(), anyList());
        verify(transactionService).markAsFailed(eq(DOC_ID), eq("Falha ao dividir o conteúdo do documento."));
    }

    @Test
    @DisplayName("TextChunker retorna null: não chama replace, chama markAsFailed com mensagem de chunking")
    void nullChunkListDoesNotCallReplace() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenReturn("texto extraído");
        when(textChunker.chunk(anyString())).thenReturn(null);

        assertThrows(Exception.class, () -> service.ingest(DOC_ID));

        verify(transactionService, never()).replaceChunksAndMarkIndexed(anyLong(), anyList());
        verify(transactionService).markAsFailed(eq(DOC_ID), eq("Falha ao dividir o conteúdo do documento."));
    }

    @Test
    @DisplayName("falha de markAsFailed não oculta a exceção original")
    void markAsFailedFailureDoesNotHideOriginal() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenThrow(
            new RuntimeException("erro original")
        );

        RuntimeException originalException = assertThrows(RuntimeException.class,
            () -> service.ingest(DOC_ID));

        assertTrue(originalException.getMessage().contains("erro original"));
    }

    @Test
    @DisplayName("falha de markAsFailed é adicionada como suprimida")
    void markAsFailedFailureIsSuppressed() throws Exception {
        Path tempFile = Files.createTempFile("ingestion-test-", ".txt");
        Files.writeString(tempFile, "conteúdo");
        tempFile.toFile().deleteOnExit();

        IngestionSource source = new IngestionSource(DOC_ID, tempFile.toString(), "text/plain");
        when(transactionService.markAsProcessing(DOC_ID)).thenReturn(source);
        when(documentParser.parse(any(), anyString())).thenThrow(
            new RuntimeException("erro original")
        );
        doThrow(new RuntimeException("markAsFailed error"))
            .when(transactionService).markAsFailed(anyLong(), anyString());

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> service.ingest(DOC_ID));

        assertEquals("erro original", thrown.getMessage());
        assertTrue(thrown.getSuppressed().length > 0);
        assertEquals("markAsFailed error", thrown.getSuppressed()[0].getMessage());
    }
}
