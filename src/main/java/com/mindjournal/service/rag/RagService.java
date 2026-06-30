package com.mindjournal.service.rag;

import com.mindjournal.config.RagRetrievalProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.service.embedding.EmbeddingService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vector.rag.repository.DocumentChunkRepository;
import vector.rag.repository.RelevantChunkProjection;

import java.util.List;

@Service
@Profile("postgres")
public class RagService {

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

    public RagContext retrieveContext(Long sessionId, String userQuery) {
        float[] vector = embeddingService.generateEmbedding(userQuery);

        int topK = retrievalProperties.getTopK();
        double minSimilarity = retrievalProperties.getMinSimilarity();

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionId,
            vector,
            minSimilarity,
            PageRequest.of(0, topK)
        );

        List<SourceDTO> sources = results.stream()
                .map(r -> new SourceDTO(
                        r.documentId(),
                        r.fileName(),
                        r.chunkId(),
                        r.content(),
                        r.similarityScore()
                ))
                .toList();

        String combinedContent = sources.stream()
                .map(SourceDTO::content)
                .collect(java.util.stream.Collectors.joining("\n\n"));

        return new RagContext(combinedContent, sources);
    }
}
