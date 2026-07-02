package com.mindjournal.repository;

import com.mindjournal.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document d JOIN FETCH d.attachment a WHERE a.session.id = :sessionId")
    List<DocumentChunk> findAllBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT c FROM DocumentChunk c
        JOIN FETCH c.document d
        JOIN FETCH d.attachment a
        WHERE a.session.id = :sessionId
        AND d.status = 'INDEXED'
        """)
    List<DocumentChunk> findAllIndexedBySessionId(@Param("sessionId") Long sessionId);
}
