package com.mindjournal.service.rag;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.entity.Document;
import com.mindjournal.exception.DocumentNotFoundException;
import com.mindjournal.exception.EmbeddingException;
import com.mindjournal.exception.EmptyDocumentException;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.service.chunking.TextChunker;
import com.mindjournal.service.embedding.EmbeddingService;
import com.mindjournal.service.parsing.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final IngestionTransactionService transactionService;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final ObjectProvider<EmbeddingService> embeddingServiceProvider;
    private final RagIngestionProperties properties;
    private final DocumentRepository documentRepository;
    private final DocumentIndexingNotifier indexingNotifier;

    public DocumentIngestionService(
        IngestionTransactionService transactionService,
        DocumentParser documentParser,
        TextChunker textChunker,
        ObjectProvider<EmbeddingService> embeddingServiceProvider,
        RagIngestionProperties properties,
        DocumentRepository documentRepository,
        DocumentIndexingNotifier indexingNotifier
    ) {
        this.transactionService = transactionService;
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingServiceProvider = embeddingServiceProvider;
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.indexingNotifier = indexingNotifier;
    }

    public void ingest(Long documentId) {
        IngestionSource source = transactionService.markAsProcessing(documentId);

        String stage = "leitura";
        try {
            stage = "leitura";
            byte[] fileBytes = readFile(source);

            stage = "parsing";
            String text = documentParser.parse(fileBytes, source.contentType());

            stage = "chunking";
            List<String> chunkTexts = textChunker.chunk(text);
            if (chunkTexts == null) {
                throw new IllegalArgumentException("Lista de chunks nula.");
            }
            chunkTexts = chunkTexts.stream()
                .map(c -> c != null ? c.strip() : "")
                .filter(c -> !c.isEmpty())
                .toList();
            if (chunkTexts.isEmpty()) {
                throw new EmptyDocumentException(
                    "O documento não produz chunks com conteúdo após a divisão."
                );
            }

            stage = "embedding";
            EmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
            if (embeddingService == null) {
                throw new EmbeddingException("Serviço de embedding não disponível.");
            }

            int dimension = properties.getEmbedding().getDimension();
            List<PreparedChunk> preparedChunks = new ArrayList<>();

            for (int i = 0; i < chunkTexts.size(); i++) {
                float[] embedding = embeddingService.generateEmbedding(chunkTexts.get(i));
                if (embedding.length != dimension) {
                    throw new EmbeddingException(
                        "Embedding com dimensão " + embedding.length + ", esperado " + dimension + "."
                    );
                }
                preparedChunks.add(new PreparedChunk(chunkTexts.get(i), i, embedding));
            }

            stage = "persistencia";
            transactionService.replaceChunksAndMarkIndexed(documentId, preparedChunks);

            notifyIndexed(documentId, preparedChunks.size());
        } catch (Exception e) {
            String safeMessage = switch (stage) {
                case "leitura" -> "Falha ao ler o arquivo do documento.";
                case "parsing" -> "Falha ao extrair o conteúdo textual do documento.";
                case "chunking" -> "Falha ao dividir o conteúdo do documento.";
                case "embedding" -> "Falha ao gerar a representação vetorial do documento.";
                case "persistencia" -> "Falha ao persistir os fragmentos do documento.";
                default -> "Ocorreu um erro durante o processamento do documento.";
            };
            try {
                transactionService.markAsFailed(documentId, safeMessage);
            } catch (Exception markFailedException) {
                e.addSuppressed(markFailedException);
            }
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }

    private void notifyIndexed(Long documentId, int indexedChunks) {
        try {
            Document document = documentRepository.findByIdWithAttachment(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));
            indexingNotifier.notifyIndexed(document, indexedChunks);
        } catch (Exception e) {
            log.warn("Falha ao notificar indexação do documento {}: {}", documentId, e.getMessage());
        }
    }

    private byte[] readFile(IngestionSource source) {
        try {
            return Files.readAllBytes(Path.of(source.filePath()));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo do documento.", e);
        }
    }
}
