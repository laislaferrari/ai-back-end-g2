package vector.rag.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vector.rag.entity.DocumentChunk;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
