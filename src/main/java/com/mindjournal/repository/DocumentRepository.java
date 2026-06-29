package com.mindjournal.repository;

import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByStatus(DocumentStatus status);

    boolean existsByAttachmentId(Long attachmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d JOIN FETCH d.attachment WHERE d.id = :id")
    Optional<Document> findByIdWithAttachmentForUpdate(@Param("id") Long id);

    @Query("SELECT d FROM Document d JOIN FETCH d.attachment WHERE d.id = :id")
    Optional<Document> findByIdWithAttachment(@Param("id") Long id);
}
