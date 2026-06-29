package com.mindjournal.service;

import com.mindjournal.dto.AttachmentDTO;
import com.mindjournal.dto.AttachmentInput;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.InvalidFileException;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.DocumentIngestionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final SessionRepository sessionRepository;
    private final DocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectProvider<DocumentIngestionService> ingestionServiceProvider;
    private final String UPLOAD_DIR = "uploads/";
    private final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    // Construtor manual (Sem Lombok)
    public AttachmentService(
        AttachmentRepository attachmentRepository,
        SessionRepository sessionRepository,
        DocumentRepository documentRepository,
        TransactionTemplate transactionTemplate,
        ObjectProvider<DocumentIngestionService> ingestionServiceProvider
    ) {
        this.attachmentRepository = attachmentRepository;
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.transactionTemplate = transactionTemplate;
        this.ingestionServiceProvider = ingestionServiceProvider;
    }

    public AttachmentDTO uploadAttachment(AttachmentInput input) {
        // 1. Validar tamanho
        if (input.size() > MAX_FILE_SIZE) {
            throw new InvalidFileException("O arquivo excede o limite máximo de 10MB.");
        }

        // 2. Validar tipo e extensão
        AttachmentType type = validateAndGetType(input.originalFilename(), input.mimeType());

        // 3. Buscar sessão
        Session session = sessionRepository.findById(input.sessionId())
                .orElseThrow(() -> new SessionNotFoundException(input.sessionId()));

        // 4. Salvar arquivo fisicamente
        String savedPath = saveFileToDisk(input.content(), input.originalFilename());

        // 5. Persistir metadados e criar Document em uma única transação
        Document document = transactionTemplate.execute(status -> {
            Attachment attachment = new Attachment();
            attachment.setSession(session);
            attachment.setFilename(input.originalFilename());
            attachment.setType(type);
            attachment.setSize(input.size());
            attachment.setFilePath(savedPath);
            attachment.setUploadDate(Instant.now());

            Attachment savedAttachment = attachmentRepository.save(attachment);

            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);

            Document doc = new Document(savedAttachment);
            return documentRepository.save(doc);
        });

        // 6. Disparar ingestão (em transação separada, após o commit acima)
        DocumentIngestionService ingestionService = ingestionServiceProvider.getIfAvailable();
        if (ingestionService != null) {
            ingestionService.ingest(document.getId());
        }

        return new AttachmentDTO(
                document.getAttachment().getId(),
                session.getId(),
                document.getAttachment().getFilename(),
                type,
                input.size(),
                document.getAttachment().getUploadDate(),
                document.getId()
        );
    }

    private AttachmentType validateAndGetType(String filename, String mimeType) {
        if (filename == null || mimeType == null) {
            throw new InvalidFileException("Arquivo inválido.");
        }
        String lowerName = filename.toLowerCase();
        
        if (lowerName.endsWith(".pdf") && mimeType.equals("application/pdf")) {
            return AttachmentType.PDF;
        } else if (lowerName.endsWith(".txt") && mimeType.equals("text/plain")) {
            return AttachmentType.TXT;
        } else {
            throw new InvalidFileException("Apenas arquivos .txt e .pdf são permitidos.");
        }
    }

    private String saveFileToDisk(byte[] content, String originalFilename) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Gera um nome único para evitar sobrescrever arquivos com o mesmo nome
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            Files.write(filePath, content);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Erro interno ao salvar o arquivo.", e);
        }
    }
}