package com.mindjournal.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    @DisplayName("DELETE /api/sessions/{id} retorna 204 para sessão existente")
    void deleteExistingSessionReturns204() throws Exception {
        Session session = sessionRepository.save(new Session("Sess\u00e3o Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id} retorna 404 para sessão inexistente")
    void deleteNonExistentSessionReturns404() throws Exception {
        mockMvc.perform(delete("/api/sessions/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Sess\u00e3o n\u00e3o encontrada"));
    }

    @Test
    @DisplayName("sess\u00e3o exclu\u00edda desaparece da listagem")
    void deletedSessionDisappearsFromListing() throws Exception {
        Session session = sessionRepository.save(new Session("Sess\u00e3o Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()));

        assertFalse(sessionRepository.existsById(session.getId()));
    }

    @Test
    @DisplayName("mensagens relacionadas s\u00e3o removidas ao excluir sess\u00e3o")
    void deletesMessagesOfSession() throws Exception {
        Session session = sessionRepository.save(new Session("Sess\u00e3o Teste"));
        messageRepository.save(new Message("Conte\u00fado", MessageRole.USER, session));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        assertTrue(
            messageRepository.findBySession_IdOrderByTimestampAsc(session.getId()).isEmpty()
        );
    }

    @Test
    @DisplayName("attachments e documents s\u00e3o removidos ao excluir sess\u00e3o")
    void deletesAttachmentsAndDocuments() throws Exception {
        Session session = sessionRepository.save(new Session("Sess\u00e3o Teste"));

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
    @DisplayName("outras sess\u00f5es permanecem intactas ap\u00f3s exclus\u00e3o")
    void otherSessionsRemainIntact() throws Exception {
        Session session1 = sessionRepository.save(new Session("Sess\u00e3o 1"));
        Session session2 = sessionRepository.save(new Session("Sess\u00e3o 2"));

        mockMvc.perform(delete("/api/sessions/{id}", session1.getId()))
            .andExpect(status().isNoContent());

        assertTrue(sessionRepository.existsById(session2.getId()));
    }

    @Test
    @DisplayName("segunda exclus\u00e3o do mesmo ID retorna 404")
    void secondDeletionReturns404() throws Exception {
        Session session = sessionRepository.save(new Session("Sess\u00e3o Teste"));

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/sessions/{id}", session.getId()))
            .andExpect(status().isNotFound());
    }
}
