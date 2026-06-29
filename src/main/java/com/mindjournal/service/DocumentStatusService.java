package com.mindjournal.service;

import com.mindjournal.dto.DocumentStatusResponse;
import com.mindjournal.entity.Document;
import com.mindjournal.exception.DocumentNotFoundException;
import com.mindjournal.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentStatusService {

    private final DocumentRepository documentRepository;

    public DocumentStatusService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(Long documentId) {
        Document document = documentRepository.findByIdWithAttachment(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        return new DocumentStatusResponse(
                document.getId(),
                document.getAttachment().getFilename(),
                document.getStatus(),
                document.getUpdatedAt()
        );
    }
}
