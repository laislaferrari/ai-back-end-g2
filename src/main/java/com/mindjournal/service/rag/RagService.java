package com.mindjournal.service.rag;

import com.mindjournal.dto.SourceDTO;
import com.mindjournal.service.embedding.EmbeddingService;
import vector.rag.entity.DocumentChunk;
import vector.rag.repository.DocumentChunkRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("postgres")
public class RagService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    public RagService(EmbeddingService embeddingService, DocumentChunkRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    public RagContext retrieveContext(String userQuery) {
        float[] vector = embeddingService.generateEmbedding(userQuery);
        
        List<DocumentChunk> chunks = chunkRepository.findTopRelevantChunks(vector, 0.0f, PageRequest.of(0, 3));

        List<SourceDTO> sources = chunks.stream().map(chunk ->
            new SourceDTO(
                chunk.getDocument().getId(),
                chunk.getDocument().getAttachment().getFilename(),
                chunk.getId(),
                chunk.getContent(), 
                0.99 
            )
        ).collect(Collectors.toList());

        String combinedContent = sources.stream()
                .map(SourceDTO::content)
                .collect(Collectors.joining("\n\n"));

        return new RagContext(combinedContent, sources);
    }

    public record RagContext(String content, List<SourceDTO> sources) {}
}