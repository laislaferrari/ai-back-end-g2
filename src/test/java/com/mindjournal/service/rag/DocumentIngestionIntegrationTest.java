package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.entity.Session;
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
import vector.rag.entity.DocumentChunk;
import vector.rag.repository.DocumentChunkRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({"postgres", "test"})
@Testcontainers
class DocumentIngestionIntegrationTest {

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
    private DocumentIngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private Session session;
    private Attachment attachment;
    private Document document;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception {
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        attachmentRepository.deleteAll();
        sessionRepository.deleteAll();

        session = new Session("Test Session for Integration");
        session = sessionRepository.save(session);

        tempFile = Files.createTempFile("integration-test-", ".txt");
        Files.writeString(tempFile, "Conteúdo de teste para o pipeline de ingestão.");

        attachment = new Attachment();
        attachment.setSession(session);
        attachment.setFilename("integration-test.txt");
        attachment.setType(AttachmentType.TXT);
        attachment.setSize(Files.size(tempFile));
        attachment.setFilePath(tempFile.toString());
        attachment.setUploadDate(Instant.now());
        attachment = attachmentRepository.save(attachment);

        document = new Document(attachment);
        document = documentRepository.save(document);
    }

    @Test
    @DisplayName("ingest executa pipeline completo e document fica INDEXED")
    void fullIngestionPipeline() {
        assertEquals(DocumentStatus.RECEIVED, document.getStatus());

        ingestionService.ingest(document.getId());

        Document updated = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals(DocumentStatus.INDEXED, updated.getStatus());
    }

    @Test
    @DisplayName("chunks são criados com índices sequenciais e ordenados")
    void chunksAreOrderedWithSequentialIndices() {
        ingestionService.ingest(document.getId());

        List<DocumentChunk> chunks = documentChunkRepository
            .findByDocumentIdOrderByChunkIndexAsc(document.getId());

        assertFalse(chunks.isEmpty());

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex(),
                "chunkIndex deve ser sequencial a partir de 0");
        }
    }

    @Test
    @DisplayName("cada chunk possui embedding com exatamente 768 posições")
    void eachChunkHas768Dimensions() {
        ingestionService.ingest(document.getId());

        List<DocumentChunk> chunks = documentChunkRepository
            .findByDocumentIdOrderByChunkIndexAsc(document.getId());

        assertFalse(chunks.isEmpty());

        for (DocumentChunk chunk : chunks) {
            assertNotNull(chunk.getEmbedding());
            assertEquals(768, chunk.getEmbedding().length,
                "Embedding deve ter 768 posições");
        }
    }

    @Test
    @DisplayName("documento em status RECEIVED é processado com sucesso")
    void receivedDocumentIsProcessed() {
        assertEquals(DocumentStatus.RECEIVED, document.getStatus());

        ingestionService.ingest(document.getId());

        Document updated = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals(DocumentStatus.INDEXED, updated.getStatus());
    }
}
