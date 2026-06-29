package com.mindjournal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.dto.AttachmentDTO;
import com.mindjournal.dto.AttachmentInput;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.InvalidFileException;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.DocumentIngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ObjectProvider<DocumentIngestionService> ingestionServiceProvider;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Captor
    private ArgumentCaptor<Attachment> attachmentCaptor;

    private AttachmentService service;

    private Session session;

    private static final Long SESSION_ID = 1L;

    private Path createdFile;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(
            attachmentRepository, sessionRepository, documentRepository,
            transactionTemplate, ingestionServiceProvider
        );

        session = new Session("Título");
        session.setId(SESSION_ID);
        session.setUpdatedAt(java.time.Instant.now());
    }

    @AfterEach
    void cleanUp() throws IOException {
        if (createdFile != null) {
            Files.deleteIfExists(createdFile);
        }
        Path uploadDir = Path.of("uploads");
        if (Files.exists(uploadDir)) {
            try (var files = Files.list(uploadDir)) {
                if (!files.findAny().isPresent()) {
                    Files.deleteIfExists(uploadDir);
                }
            }
        }
    }

    @Test
    @DisplayName("uploadAttachment cria Attachment e Document com status RECEIVED")
    void uploadAttachmentCreatesDocumentWithReceived() {
        byte[] content = "conteúdo do arquivo".getBytes();
        AttachmentInput input = new AttachmentInput(
            "relatorio.txt", "text/plain", content.length, content, SESSION_ID
        );

        when(sessionRepository.findById(SESSION_ID)).thenReturn(java.util.Optional.of(session));

        Attachment savedAttachment = new Attachment();
        savedAttachment.setId(100L);
        savedAttachment.setSession(session);
        savedAttachment.setFilename("relatorio.txt");
        savedAttachment.setType(AttachmentType.TXT);
        savedAttachment.setSize((long) content.length);
        savedAttachment.setFilePath("uploads/unique_relatorio.txt");
        savedAttachment.setUploadDate(java.time.Instant.now());

        when(attachmentRepository.save(any(Attachment.class))).thenReturn(savedAttachment);

        Document savedDocument = new Document(savedAttachment);
        ReflectionTestUtils.setField(savedDocument, "id", 200L);

        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Document> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(ingestionServiceProvider.getIfAvailable()).thenReturn(null);

        AttachmentDTO result = service.uploadAttachment(input);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals(SESSION_ID, result.sessionId());
        assertEquals("relatorio.txt", result.filename());
        assertEquals(AttachmentType.TXT, result.type());
        assertNotNull(result.documentId());
        assertEquals(200L, result.documentId());

        verify(attachmentRepository).save(attachmentCaptor.capture());
        createdFile = Path.of(attachmentCaptor.getValue().getFilePath());

        verify(documentRepository).save(documentCaptor.capture());
        Document captured = documentCaptor.getValue();
        assertEquals(DocumentStatus.RECEIVED, captured.getStatus());
        assertNotNull(captured.getAttachment());
    }

    @Test
    @DisplayName("uploadAttachment com sessionId inexistente lança SessionNotFoundException")
    void uploadAttachmentThrowsSessionNotFound() {
        AttachmentInput input = new AttachmentInput(
            "relatorio.txt", "text/plain", 100, "teste".getBytes(), 999L
        );

        when(sessionRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> service.uploadAttachment(input));
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadAttachment com tipo inválido lança InvalidFileException")
    void uploadAttachmentThrowsInvalidFile() {
        AttachmentInput input = new AttachmentInput(
            "arquivo.exe", "application/x-msdownload", 100, "teste".getBytes(), SESSION_ID
        );

        assertThrows(InvalidFileException.class, () -> service.uploadAttachment(input));
        verify(documentRepository, never()).save(any());
    }
}
