package vector.rag.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vector.rag.entity.DocumentChunk;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT c FROM DocumentChunk c " +
           "WHERE 1 - cosine_distance(c.embedding, :embedding) >= :minSimilarity " +
           "ORDER BY cosine_distance(c.embedding, :embedding)")
    List<DocumentChunk> findTopRelevantChunks(
        @Param("embedding") float[] embedding,
        @Param("minSimilarity") float minSimilarity,
        Pageable pageable
    );

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
    List<Object[]> findRelevantChunksNative(
        @Param("sessionId") Long sessionId,
        @Param("embedding") float[] embedding,
        @Param("minSimilarity") double minSimilarity,
        Pageable pageable
    );

    default List<RelevantChunkProjection> findRelevantChunks(
            Long sessionId, float[] embedding, double minSimilarity, Pageable pageable) {
        return findRelevantChunksNative(sessionId, embedding, minSimilarity, pageable)
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
}
