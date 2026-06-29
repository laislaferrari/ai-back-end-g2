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
}
