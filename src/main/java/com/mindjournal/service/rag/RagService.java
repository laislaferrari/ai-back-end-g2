package com.mindjournal.service.rag;

import com.mindjournal.config.RagRetrievalProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.DocumentChunk;
import com.mindjournal.repository.DocumentChunkRepository;
import com.mindjournal.service.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final RagRetrievalProperties retrievalProperties;

    public RagService(EmbeddingService embeddingService,
                      DocumentChunkRepository chunkRepository,
                      RagRetrievalProperties retrievalProperties) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.retrievalProperties = retrievalProperties;
    }

    @Transactional(readOnly = true)
    public RagContext retrieveContext(Long sessionId, String userQuery) {
        float[] queryEmbedding = embeddingService.generateEmbedding(userQuery);

        int topK = retrievalProperties.getTopK();
        double minSimilarity = retrievalProperties.getMinSimilarity();

        List<DocumentChunk> allChunks = chunkRepository.findAllBySessionId(sessionId);

        log.debug("RAG: sessionId={}, totalChunks={}, topK={}, minSimilarity={}",
                sessionId, allChunks.size(), topK, minSimilarity);

        if (allChunks.isEmpty()) {
            log.warn("RAG: nenhum chunk encontrado para sessionId={}", sessionId);
            return new RagContext("", List.of());
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : allChunks) {
            double similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            if (similarity >= minSimilarity) {
                scored.add(new ScoredChunk(chunk, similarity));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::similarity).reversed());

        List<ScoredChunk> topResults = scored.size() > topK ? scored.subList(0, topK) : scored;

        log.info("RAG: sessionId={}, totalChunks={}, aboveThreshold={}, returned={}",
                sessionId, allChunks.size(), scored.size(), topResults.size());

        List<SourceDTO> sources = topResults.stream()
                .map(s -> new SourceDTO(
                        s.chunk().getDocument().getId(),
                        s.chunk().getDocument().getAttachment().getFilename(),
                        s.chunk().getId(),
                        s.chunk().getContent(),
                        s.similarity()
                ))
                .toList();

        String combinedContent = sources.stream()
                .map(SourceDTO::content)
                .collect(java.util.stream.Collectors.joining("\n\n"));

        return new RagContext(combinedContent, sources);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record ScoredChunk(DocumentChunk chunk, double similarity) {}
}
