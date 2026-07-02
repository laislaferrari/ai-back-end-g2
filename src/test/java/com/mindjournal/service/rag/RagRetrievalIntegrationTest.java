package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.config.RagRetrievalProperties;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentChunk;
import com.mindjournal.entity.Session;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentChunkRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class RagRetrievalIntegrationTest {

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EmbeddingService embeddingService;

    private RagRetrievalProperties retrievalProperties;
    private RagService ragService;

    private Session sessionA;
    private Session sessionB;

    @BeforeEach
    void setUp() throws Exception {
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        attachmentRepository.deleteAll();
        sessionRepository.deleteAll();

        retrievalProperties = new RagRetrievalProperties();
        retrievalProperties.setTopK(10);
        retrievalProperties.setMinSimilarity(0.0);
        ragService = new RagService(embeddingService, chunkRepository, retrievalProperties);

        sessionA = sessionRepository.save(new Session("Sessao A"));
        sessionB = sessionRepository.save(new Session("Sessao B"));

        createDocumentWithChunks(sessionA, "doc-a.txt", "conteudo A");
        createDocumentWithChunks(sessionB, "doc-b.txt", "conteudo B");
    }

    private void createDocumentWithChunks(Session session, String filename,
                                          String content) throws Exception {
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

        float[] embedding = embeddingService.generateEmbedding(content);
        DocumentChunk chunk = new DocumentChunk(document, content, 0, embedding);
        chunkRepository.save(chunk);
    }

    @Test
    @DisplayName("consulta para sessão A não retorna chunks da sessão B")
    void sessionIsolation() {
        RagContext ctx = ragService.retrieveContext(sessionA.getId(), "conteudo A");

        assertFalse(ctx.sources().isEmpty());
        for (SourceDTO s : ctx.sources()) {
            assertEquals("doc-a.txt", s.fileName(),
                "deve retornar apenas chunks da sessão A, mas veio " + s.fileName());
        }
    }

    @Test
    @DisplayName("consulta para sessão B não retorna chunks da sessão A")
    void sessionIsolationReverse() {
        RagContext ctx = ragService.retrieveContext(sessionB.getId(), "conteudo B");

        assertFalse(ctx.sources().isEmpty());
        for (SourceDTO s : ctx.sources()) {
            assertEquals("doc-b.txt", s.fileName(),
                "deve retornar apenas chunks da sessão B, mas veio " + s.fileName());
        }
    }

    @Test
    @DisplayName("similarityScore está entre 0 e 1")
    void scoreBetweenZeroAndOne() {
        RagContext ctx = ragService.retrieveContext(sessionA.getId(), "conteudo A");

        assertFalse(ctx.sources().isEmpty());
        for (SourceDTO s : ctx.sources()) {
            assertTrue(s.similarityScore() >= 0.0 && s.similarityScore() <= 1.0001,
                "similarityScore deve estar entre 0 e 1, mas foi " + s.similarityScore());
        }
    }

    @Test
    @DisplayName("resultados ordenados do maior para o menor score")
    void resultsOrderedByScoreDesc() throws Exception {
        Session sessionC = sessionRepository.save(new Session("Sessao C"));
        createDocumentWithChunks(sessionC, "doc-c-1.txt", "similar");
        createDocumentWithChunks(sessionC, "doc-c-2.txt", "muito similar");

        RagContext ctx = ragService.retrieveContext(sessionC.getId(), "similar");

        assertTrue(ctx.sources().size() >= 2);
        for (int i = 0; i < ctx.sources().size() - 1; i++) {
            assertTrue(ctx.sources().get(i).similarityScore() >= ctx.sources().get(i + 1).similarityScore(),
                "ordem decrescente esperada no índice " + i);
        }
    }

    @Test
    @DisplayName("topK limita a quantidade de resultados")
    void topKLimitsResults() throws Exception {
        Session sessionD = sessionRepository.save(new Session("Sessao D"));
        for (int i = 0; i < 5; i++) {
            createDocumentWithChunks(sessionD, "doc-d-" + i + ".txt", "conteudo " + i);
        }

        retrievalProperties.setTopK(3);
        RagContext ctx = ragService.retrieveContext(sessionD.getId(), "conteudo");

        assertEquals(3, ctx.sources().size(), "topK deve limitar a 3 resultados");
    }

    @Test
    @DisplayName("minSimilarity remove resultados abaixo do limiar")
    void minSimilarityFiltersBelowThreshold() throws Exception {
        Session sessionE = sessionRepository.save(new Session("Sessao E"));
        createDocumentWithChunks(sessionE, "doc-e-similar.txt", "este conteúdo é muito similar");
        createDocumentWithChunks(sessionE, "doc-e-diferente.txt", "algo completamente diferente");

        retrievalProperties.setMinSimilarity(0.80);
        RagContext ctx = ragService.retrieveContext(sessionE.getId(), "este conteúdo é muito similar");

        assertFalse(ctx.sources().isEmpty());
        for (SourceDTO s : ctx.sources()) {
            assertTrue(s.similarityScore() >= 0.80,
                "similarityScore " + s.similarityScore() + " deve ser >= 0.80");
        }
    }

    @Test
    @DisplayName("consulta retorna chunkId, documentId, fileName e content corretos")
    void resultContainsCorrectFields() {
        RagContext ctx = ragService.retrieveContext(sessionA.getId(), "conteudo A");

        assertFalse(ctx.sources().isEmpty());
        SourceDTO s = ctx.sources().get(0);
        assertNotNull(s.chunkId());
        assertNotNull(s.documentId());
        assertEquals("doc-a.txt", s.fileName());
        assertEquals("conteudo A", s.content());
    }
}
