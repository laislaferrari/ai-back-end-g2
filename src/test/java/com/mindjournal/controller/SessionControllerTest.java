package com.mindjournal.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.Message;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanTempFiles() {
        for (Path path : tempFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                // ignora — o arquivo pode já ter sido excluído pelo teste
            }
        }
        tempFiles.clear();
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id} retorna 204 para sessão existente")
    void deleteExistingSessionReturns204() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id} retorna 404 para sessão inexistente")
    void deleteNonExistentSessionReturns404() throws Exception {
        mockMvc.perform(delete("/api/sessions/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Sessão não encontrada"));
    }

    @Test
    @DisplayName("sessão excluída desaparece da listagem")
    void deletedSessionDisappearsFromListing() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()));

        assertFalse(sessionRepository.existsById(session.getId()));
    }

    @Test
    @DisplayName("mensagens relacionadas são removidas ao excluir sessão")
    void deletesMessagesOfSession() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));
        messageRepository.save(new Message("Conteúdo", MessageRole.USER, session));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        assertTrue(messageRepository.findBySession_IdOrderByTimestampAsc(session.getId()).isEmpty());
    }

    @Test
    @DisplayName("attachments e documents são removidos ao excluir sessão")
    void deletesAttachmentsAndDocuments() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));

        Attachment attachment = new Attachment();
        attachment.setSession(session);
        attachment.setFilename("teste.txt");
        attachment.setType(AttachmentType.TXT);
        attachment.setSize(100L);
        attachment.setFilePath("/tmp/teste.txt");
        attachment.setUploadDate(Instant.now());
        attachment = attachmentRepository.save(attachment);

        Document document = new Document(attachment);
        documentRepository.save(document);

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        assertFalse(attachmentRepository.findById(attachment.getId()).isPresent());
        assertFalse(documentRepository.findById(document.getId()).isPresent());
    }

    @Test
    @DisplayName("outras sessões permanecem intactas após exclusão")
    void otherSessionsRemainIntact() throws Exception {
        Session session1 = sessionRepository.save(new Session("Sessão 1"));
        Session session2 = sessionRepository.save(new Session("Sessão 2"));

        mockMvc.perform(delete("/api/sessions/{id}", session1.getId()))
            .andExpect(status().isNoContent());

        assertTrue(sessionRepository.existsById(session2.getId()));
    }

    @Test
    @DisplayName("segunda exclusão do mesmo ID retorna 404")
    void secondDeletionReturns404() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id} remove arquivos físicos dos anexos")
    void deleteSessionRemovesPhysicalFiles() throws Exception {
        Session session = sessionRepository.save(new Session("Sessão Teste"));

        Path uploadDir = Path.of("uploads");
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve("fisico_" + System.nanoTime() + ".txt");
        Files.writeString(filePath, "conteúdo");

        tempFiles.add(filePath);

        Attachment attachment = new Attachment();
        attachment.setSession(session);
        attachment.setFilename("fisico.txt");
        attachment.setType(AttachmentType.TXT);
        attachment.setSize(100L);
        attachment.setFilePath(filePath.toString());
        attachment.setUploadDate(Instant.now());
        attachmentRepository.save(attachment);

        assertTrue(Files.exists(filePath));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        assertFalse(Files.exists(filePath));
    }

    @Test
    @DisplayName("PATCH /api/sessions/{id}/title atualiza título")
    void updateTitleReturnsUpdatedSession() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));

        mockMvc.perform(patch("/api/sessions/{id}/title", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Meu novo título\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(session.getId()))
            .andExpect(jsonPath("$.title").value("Meu novo título"));
    }

    @Test
    @DisplayName("PATCH /api/sessions/{id}/title retorna 400 para título vazio")
    void updateTitleRejectsEmptyTitle() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));

        mockMvc.perform(patch("/api/sessions/{id}/title", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    @DisplayName("PATCH /api/sessions/{id}/title retorna 400 para título muito longo")
    void updateTitleRejectsTitleTooLong() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));
        String longTitle = "a".repeat(151);

        mockMvc.perform(patch("/api/sessions/{id}/title", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + longTitle + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/sessions/{id}/title retorna 404 para sessão inexistente")
    void updateTitleReturns404ForNonExistentSession() throws Exception {
        mockMvc.perform(patch("/api/sessions/{id}/title", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Título\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Sessão não encontrada"));
    }
}