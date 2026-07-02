package com.mindjournal.service.rag;

import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentChunk;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.exception.DocumentNotFoundException;
import com.mindjournal.repository.DocumentChunkRepository;
import com.mindjournal.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IngestionTransactionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public IngestionTransactionService(
        DocumentRepository documentRepository,
        DocumentChunkRepository documentChunkRepository
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionSource markAsProcessing(Long documentId) {
        Document document = documentRepository.findByIdWithAttachmentForUpdate(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        document.markAsProcessing();
        documentRepository.flush();

        var attachment = document.getAttachment();
        String contentType = switch (attachment.getType()) {
            case TXT -> "text/plain";
            case PDF -> "application/pdf";
        };

        return new IngestionSource(documentId, attachment.getFilePath(), contentType);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceChunksAndMarkIndexed(
        Long documentId,
        List<PreparedChunk> preparedChunks
    ) {
        if (preparedChunks == null || preparedChunks.isEmpty()) {
            throw new IllegalArgumentException("A lista de chunks não pode ser nula ou vazia.");
        }

        Document document = documentRepository.findByIdWithAttachmentForUpdate(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (document.getStatus() != DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                "Documento " + documentId + " não está em PROCESSING (status atual: " + document.getStatus() + ")."
            );
        }

        documentChunkRepository.deleteByDocumentId(documentId);
        documentChunkRepository.flush();

        List<DocumentChunk> chunks = preparedChunks.stream()
            .map(pc -> new DocumentChunk(
                document,
                pc.content(),
                pc.chunkIndex(),
                pc.embedding()
            ))
            .toList();

        documentChunkRepository.saveAll(chunks);
        documentChunkRepository.flush();

        document.markAsIndexed();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long documentId, String safeMessage) {
        Document document = documentRepository.findByIdWithAttachmentForUpdate(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (document.getStatus() != DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                "Documento " + documentId + " não está em PROCESSING (status atual: " + document.getStatus() + ")."
            );
        }

        if (safeMessage == null || safeMessage.isBlank()) {
            safeMessage = "Ocorreu um erro durante o processamento do documento.";
        }

        safeMessage = safeMessage.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
        if (safeMessage.length() > 500) {
            safeMessage = safeMessage.substring(0, 500);
        }

        document.markAsFailed(safeMessage);
    }
}
