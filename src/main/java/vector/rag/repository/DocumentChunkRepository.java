package vector.rag.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vector.rag.entity.DocumentChunk;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM DocumentChunk c " +
           "WHERE 1 - (c.embedding <=> :embedding) >= :minSimilarity " +
           "ORDER BY c.embedding <=> :embedding " +
           "LIMIT :limit")
    java.util.List<vector.rag.entity.DocumentChunk> findTopRelevantChunks(
        @org.springframework.data.repository.query.Param("embedding") float[] embedding, 
        @org.springframework.data.repository.query.Param("minSimilarity") float minSimilarity, 
        @org.springframework.data.repository.query.Param("limit") int limit
    );
}
