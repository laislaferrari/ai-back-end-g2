package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vector.rag.entity.DocumentChunk;
import vector.rag.repository.DocumentChunkRepository;
import vector.rag.repository.RelevantChunkProjection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({"postgres", "test"})
@Testcontainers
class RagRetrievalIntegrationTest {

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
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Session sessionA;
    private Session sessionB;

    @BeforeEach
    void setUp() throws Exception {
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        attachmentRepository.deleteAll();
        sessionRepository.deleteAll();

        sessionA = sessionRepository.save(new Session("Sessao A"));
        sessionB = sessionRepository.save(new Session("Sessao B"));

        createDocumentWithChunks(sessionA, "doc-a.txt", "conteudo A", vector(0.9f, 0.1f, 0.1f));
        createDocumentWithChunks(sessionB, "doc-b.txt", "conteudo B", vector(0.1f, 0.9f, 0.1f));
    }

    private void createDocumentWithChunks(Session session, String filename,
                                          String content, float[] baseVector) throws Exception {
        Path tempFile = Files.createTempFile("retrieval-", ".txt");
        Files.writeString(tempFile, content);

        Attachment attachment = new Attachment();
        attachment.setSession(session);
        attachment.setFilename(filename);
        attachment.setType(AttachmentType.TXT);
        attachment.setSize(Files.size(tempFile));
        attachment.setFilePath(tempFile.toString());
        attachment.setUploadDate(Instant.now());
        attachment = attachmentRepository.save(attachment);

        Document document = new Document(attachment);
        document.markAsProcessing();
        document.markAsIndexed();
        document = documentRepository.save(document);

        DocumentChunk chunk = new DocumentChunk(document, content, 0, baseVector);
        chunkRepository.save(chunk);
    }

    private static float[] vector(float... values) {
        float[] result = new float[768];
        for (int i = 0; i < values.length && i < 768; i++) {
            result[i] = values[i];
        }
        return result;
    }

    @Test
    @DisplayName("consulta para sessão A não retorna chunks da sessão B")
    void sessionIsolation() {
        float[] query = vector(0.9f, 0.1f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionA.getId(), query, 0.0, PageRequest.of(0, 10));

        assertFalse(results.isEmpty());
        for (RelevantChunkProjection r : results) {
            assertEquals("doc-a.txt", r.fileName(),
                "deve retornar apenas chunks da sessão A, mas veio " + r.fileName());
        }
    }

    @Test
    @DisplayName("consulta para sessão B não retorna chunks da sessão A")
    void sessionIsolationReverse() {
        float[] query = vector(0.1f, 0.9f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionB.getId(), query, 0.0, PageRequest.of(0, 10));

        assertFalse(results.isEmpty());
        for (RelevantChunkProjection r : results) {
            assertEquals("doc-b.txt", r.fileName(),
                "deve retornar apenas chunks da sessão B, mas veio " + r.fileName());
        }
    }

    @Test
    @DisplayName("similarityScore está entre 0 e 1")
    void scoreBetweenZeroAndOne() {
        float[] query = vector(0.9f, 0.1f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionA.getId(), query, 0.0, PageRequest.of(0, 10));

        assertFalse(results.isEmpty());
        for (RelevantChunkProjection r : results) {
            assertTrue(r.similarityScore() >= 0.0 && r.similarityScore() <= 1.0,
                "similarityScore deve estar entre 0 e 1, mas foi " + r.similarityScore());
        }
    }

    @Test
    @DisplayName("resultados ordenados do maior para o menor score")
    void resultsOrderedByScoreDesc() throws Exception {
        Session sessionC = sessionRepository.save(new Session("Sessao C"));
        createDocumentWithChunks(sessionC, "doc-c-1.txt", "similar", vector(0.8f, 0.1f, 0.1f));
        createDocumentWithChunks(sessionC, "doc-c-2.txt", "menos similar", vector(0.3f, 0.2f, 0.1f));

        float[] query = vector(0.8f, 0.1f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionC.getId(), query, 0.0, PageRequest.of(0, 10));

        assertTrue(results.size() >= 2);
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).similarityScore() >= results.get(i + 1).similarityScore(),
                "ordem decrescente esperada no índice " + i);
        }
    }

    @Test
    @DisplayName("topK limita a quantidade de resultados")
    void topKLimitsResults() throws Exception {
        Session sessionD = sessionRepository.save(new Session("Sessao D"));
        for (int i = 0; i < 5; i++) {
            float offset = 0.1f * i;
            createDocumentWithChunks(sessionD, "doc-d-" + i + ".txt",
                "conteudo " + i, vector(0.9f - offset, 0.1f + offset, 0.1f));
        }

        float[] query = vector(0.9f, 0.1f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionD.getId(), query, 0.0, PageRequest.of(0, 3));

        assertEquals(3, results.size(), "topK deve limitar a 3 resultados");
    }

    @Test
    @DisplayName("minSimilarity remove resultados abaixo do limiar")
    void minSimilarityFiltersBelowThreshold() throws Exception {
        Session sessionE = sessionRepository.save(new Session("Sessao E"));
        createDocumentWithChunks(sessionE, "doc-e-similar.txt", "similar", vector(0.9f, 0.1f, 0.1f));
        createDocumentWithChunks(sessionE, "doc-e-diferente.txt", "diferente", vector(0.1f, 0.1f, 0.9f));

        float[] query = vector(0.9f, 0.1f, 0.1f);

        List<RelevantChunkProjection> allResults = chunkRepository.findRelevantChunks(
            sessionE.getId(), query, 0.0, PageRequest.of(0, 10));

        assertTrue(allResults.size() >= 2, "deve haver ambos os chunks sem filtro");

        List<RelevantChunkProjection> filteredResults = chunkRepository.findRelevantChunks(
            sessionE.getId(), query, 0.80, PageRequest.of(0, 10));

        assertFalse(filteredResults.isEmpty());
        for (RelevantChunkProjection r : filteredResults) {
            assertTrue(r.similarityScore() >= 0.80,
                "similarityScore " + r.similarityScore() + " deve ser >= 0.80");
        }
    }

    @Test
    @DisplayName("consulta retorna chunkId, documentId, fileName e content corretos")
    void resultContainsCorrectFields() {
        float[] query = vector(0.9f, 0.1f, 0.1f);

        List<RelevantChunkProjection> results = chunkRepository.findRelevantChunks(
            sessionA.getId(), query, 0.0, PageRequest.of(0, 10));

        assertFalse(results.isEmpty());
        RelevantChunkProjection r = results.get(0);
        assertNotNull(r.chunkId());
        assertNotNull(r.documentId());
        assertEquals("doc-a.txt", r.fileName());
        assertEquals("conteudo A", r.content());
        assertTrue(r.similarityScore() > 0.5);
    }
}
