package com.mindjournal.service.rag;

import com.mindjournal.config.RagIngestionProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.service.embedding.EmbeddingService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vector.rag.entity.DocumentChunk;
import vector.rag.repository.DocumentChunkRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("postgres")
public class RagService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final RagIngestionProperties properties;

    public RagService(
            EmbeddingService embeddingService,
            DocumentChunkRepository chunkRepository,
            RagIngestionProperties properties
    ) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public RagContext retrieveContext(String userQuery) {
        float[] vector = embeddingService.generateEmbedding(userQuery);

        int topK = properties.getRetrieval().getTopK();
        float minSimilarity = (float) properties.getRetrieval().getMinSimilarity();

        List<Object[]> results = chunkRepository.findRelevantChunksWithScore(
                vector, minSimilarity, PageRequest.of(0, topK)
        );

        if (results == null || results.isEmpty()) {
            return new RagContext("", Collections.emptyList());
        }

        List<SourceDTO> sources = results.stream().map(row -> {
            DocumentChunk chunk = (DocumentChunk) row[0];
            double score = (Double) row[1];
            return new SourceDTO(
                    chunk.getDocument().getId(),
                    chunk.getDocument().getAttachment().getFilename(),
                    chunk.getId(),
                    chunk.getContent(),
                    score
            );
        }).collect(Collectors.toList());

        String combinedContent = sources.stream()
                .map(SourceDTO::content)
                .collect(Collectors.joining("\n\n"));

        return new RagContext(combinedContent, sources);
    }

    public record RagContext(String content, List<SourceDTO> sources) {}
}