package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.DocumentNotFoundException;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.mindjournal.entity.DocumentChunk;
import com.mindjournal.repository.DocumentChunkRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({"postgres", "test"})
@Testcontainers
class TransactionalIngestionTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private IngestionTransactionService transactionService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private Session session;

    @BeforeEach
    void setUp() {
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        attachmentRepository.deleteAll();
        sessionRepository.deleteAll();

        session = new Session("Test Session");
        session = sessionRepository.save(session);
    }

    private Attachment createAttachment() throws Exception {
        Path tempFile = Files.createTempFile("trans-test-", ".txt");
        Files.writeString(tempFile, "conteúdo de teste");
        tempFile.toFile().deleteOnExit();

        Attachment attachment = new Attachment();
        attachment.setSession(session);
        attachment.setFilename("test.txt");
        attachment.setType(AttachmentType.TXT);
        attachment.setSize(Files.size(tempFile));
        attachment.setFilePath(tempFile.toString());
        attachment.setUploadDate(Instant.now());
        return attachmentRepository.save(attachment);
    }

    @Test
    @DisplayName("estado diferente de RECEIVED não avança para PROCESSING")
    void nonReceivedStatusDoesNotBecomeProcessing() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        doc.markAsProcessing();
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        assertThrows(IllegalStateException.class,
            () -> transactionService.markAsProcessing(id));

        Document reloaded = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.PROCESSING, reloaded.getStatus());
    }

    @Test
    @DisplayName("documento INDEXED não pode ser marcado como PROCESSING")
    void indexedDocumentCannotBeProcessing() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        doc.markAsProcessing();
        doc.markAsIndexed();
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        assertThrows(IllegalStateException.class,
            () -> transactionService.markAsProcessing(id));

        Document reloaded = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.INDEXED, reloaded.getStatus());
    }

    @Test
    @DisplayName("documento inexistente lança DocumentNotFoundException")
    void nonExistentDocumentThrowsException() {
        assertThrows(DocumentNotFoundException.class,
            () -> transactionService.markAsProcessing(999L));
    }

    @Test
    @DisplayName("falha após PROCESSING gera status FAILED")
    void failureAfterProcessingResultsInFailed() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        IngestionSource source = transactionService.markAsProcessing(id);
        assertNotNull(source);

        Document processingDoc = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.PROCESSING, processingDoc.getStatus());

        transactionService.markAsFailed(id, "Falha simulada");

        Document failedDoc = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failedDoc.getStatus());
        assertNotNull(failedDoc.getErrorMessage());
    }

    @Test
    @DisplayName("markAsFailed só permite PROCESSING")
    void markAsFailedOnlyFromProcessing() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        assertThrows(IllegalStateException.class,
            () -> transactionService.markAsFailed(id, "erro"));
    }

    @Test
    @DisplayName("reprocessamento após reset substitui chunks anteriores")
    void reprocessReplacesOldChunks() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        DocumentChunk oldChunk = new DocumentChunk(doc, "chunk original", 0, new float[768]);
        documentChunkRepository.save(oldChunk);

        transactionService.markAsProcessing(id);
        transactionService.replaceChunksAndMarkIndexed(id,
            List.of(new PreparedChunk("chunk original", 0, new float[768])));

        doc = documentRepository.findById(id).orElseThrow();
        doc.resetForReprocessing();
        documentRepository.save(doc);

        transactionService.markAsProcessing(id);

        List<PreparedChunk> newChunks = List.of(
            new PreparedChunk("chunk novo 1", 0, new float[768]),
            new PreparedChunk("chunk novo 2", 1, new float[768])
        );

        transactionService.replaceChunksAndMarkIndexed(id, newChunks);

        Document indexed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.INDEXED, indexed.getStatus());

        List<DocumentChunk> remaining = documentChunkRepository
            .findByDocumentIdOrderByChunkIndexAsc(id);

        assertEquals(2, remaining.size());
        assertEquals("chunk novo 1", remaining.get(0).getContent());
        assertEquals("chunk novo 2", remaining.get(1).getContent());
    }

    @Test
    @DisplayName("falha na persistência não deixa chunks parciais")
    void failedPersistenceDoesNotLeavePartialChunks() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        DocumentChunk original = new DocumentChunk(doc, "chunk preservado", 0, new float[768]);
        documentChunkRepository.save(original);
        documentChunkRepository.flush();

        transactionService.markAsProcessing(id);

        PreparedChunk chunk1 = new PreparedChunk("duplicado A", 0, new float[768]);
        PreparedChunk chunk2 = new PreparedChunk("duplicado B", 0, new float[768]);

        assertThrows(Exception.class,
            () -> transactionService.replaceChunksAndMarkIndexed(id,
                List.of(chunk1, chunk2)));

        List<DocumentChunk> remaining = documentChunkRepository
            .findByDocumentIdOrderByChunkIndexAsc(id);

        assertEquals(1, remaining.size());
        assertEquals("chunk preservado", remaining.get(0).getContent());
    }

    @Test
    @DisplayName("markAsFailed com mensagem nula usa mensagem genérica")
    void markAsFailedWithNullMessage() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        transactionService.markAsProcessing(id);
        transactionService.markAsFailed(id, null);

        Document failed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failed.getStatus());
        assertEquals("Ocorreu um erro durante o processamento do documento.",
            failed.getErrorMessage());
    }

    @Test
    @DisplayName("markAsFailed com mensagem vazia usa mensagem genérica")
    void markAsFailedWithBlankMessage() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        transactionService.markAsProcessing(id);
        transactionService.markAsFailed(id, "   ");

        Document failed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failed.getStatus());
        assertEquals("Ocorreu um erro durante o processamento do documento.",
            failed.getErrorMessage());
    }

    @Test
    @DisplayName("markAsFailed remove quebras de linha")
    void markAsFailedRemovesLineBreaks() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        transactionService.markAsProcessing(id);
        transactionService.markAsFailed(id, "linha1\nlinha2\r\nlinha3");

        Document failed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failed.getStatus());
        assertEquals("linha1 linha2 linha3", failed.getErrorMessage());
    }

    @Test
    @DisplayName("markAsFailed trunca mensagem com mais de 500 caracteres")
    void markAsFailedTruncatesLongMessage() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        String longMessage = "x".repeat(1000);

        transactionService.markAsProcessing(id);
        transactionService.markAsFailed(id, longMessage);

        Document failed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failed.getStatus());
        assertEquals(500, failed.getErrorMessage().length());
    }

    @Test
    @DisplayName("Document permanece FAILED após o commit")
    void documentRemainsFailedAfterCommit() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        transactionService.markAsProcessing(id);
        transactionService.markAsFailed(id, "Erro grave");

        Document failed = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.FAILED, failed.getStatus());
        assertEquals("Erro grave", failed.getErrorMessage());
    }

    @Test
    @DisplayName("IngestionSource contém dados corretos fora da transação")
    void ingestionSourceOutsideTransaction() throws Exception {
        Attachment att = createAttachment();
        Document doc = new Document(att);
        Document saved = documentRepository.save(doc);
        Long id = saved.getId();

        IngestionSource source = transactionService.markAsProcessing(id);

        assertNotNull(source);
        assertEquals(id, source.documentId());
        assertEquals(att.getFilePath(), source.filePath());
        assertEquals("text/plain", source.contentType());

        Document processingDoc = documentRepository.findById(id).orElseThrow();
        assertEquals(DocumentStatus.PROCESSING, processingDoc.getStatus());
    }
}
