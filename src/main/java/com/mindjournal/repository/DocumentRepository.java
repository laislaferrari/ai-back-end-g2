package com.mindjournal.repository;

import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByStatus(DocumentStatus status);

    boolean existsByAttachmentId(Long attachmentId);
}
