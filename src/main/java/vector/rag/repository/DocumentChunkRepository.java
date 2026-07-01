package vector.rag.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vector.rag.entity.DocumentChunk;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query(value = """
        SELECT c.*
        FROM document_chunks c
        WHERE 1 - (c.embedding <=> CAST(:embedding AS vector(768))) >= :minSimilarity
        ORDER BY c.embedding <=> CAST(:embedding AS vector(768))
        """, nativeQuery = true)
    List<DocumentChunk> findTopRelevantChunksNative(
        @Param("embedding") String embedding,
        @Param("minSimilarity") double minSimilarity,
        Pageable pageable
    );

    default List<DocumentChunk> findTopRelevantChunks(
            float[] embedding, double minSimilarity, Pageable pageable) {
        return findTopRelevantChunksNative(toVectorString(embedding), minSimilarity, pageable);
    }

    @Query(value = """
        SELECT c.id AS chunk_id,
               d.id AS document_id,
               a.filename AS file_name,
               c.content AS content,
               1 - (c.embedding <=> CAST(:embedding AS vector(768))) AS similarity_score
        FROM document_chunks c
        JOIN documents d ON d.id = c.document_id
        JOIN attachments a ON a.id = d.attachment_id
        WHERE a.session_id = :sessionId
          AND 1 - (c.embedding <=> CAST(:embedding AS vector(768))) >= :minSimilarity
        ORDER BY c.embedding <=> CAST(:embedding AS vector(768))
        """, nativeQuery = true)
    List<Object[]> findRelevantChunksBySessionNative(
        @Param("sessionId") Long sessionId,
        @Param("embedding") String embedding,
        @Param("minSimilarity") double minSimilarity,
        Pageable pageable
    );

    default List<RelevantChunkProjection> findRelevantChunks(
            Long sessionId, float[] embedding, double minSimilarity, Pageable pageable) {
        return findRelevantChunksBySessionNative(
                sessionId, toVectorString(embedding), minSimilarity, pageable)
                .stream()
                .map(row -> new RelevantChunkProjection(
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        ((Number) row[0]).longValue(),
                        (String) row[3],
                        ((Number) row[4]).doubleValue()
                ))
                .toList();
    }

    private static String toVectorString(float[] embedding) {
        return IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
