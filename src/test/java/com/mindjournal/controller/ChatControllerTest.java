package com.mindjournal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindjournal.entity.Session;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    @DisplayName("POST /api/chat com acentos retorna 200")
    void chatWithAccentsReturns200() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));

        String json = """
                {
                    "sessionId": %d,
                    "content": "Olá, você pode responder em português?"
                }
                """.formatted(session.getId());

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userMessage").exists())
            .andExpect(jsonPath("$.assistantMessage").exists())
            .andExpect(jsonPath("$.sessionTitle").exists());
    }

    @Test
    @DisplayName("POST /api/chat com sessão inexistente retorna 404")
    void chatWithNonExistentSessionReturns404() throws Exception {
        String json = """
                {
                    "sessionId": 99999,
                    "content": "Olá"
                }
                """;

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Sessão não encontrada"));
    }

    @Test
    @DisplayName("POST /api/chat com conteúdo vazio retorna 400")
    void chatWithEmptyContentReturns400() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));

        String json = """
                {
                    "sessionId": %d,
                    "content": ""
                }
                """.formatted(session.getId());

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    @DisplayName("POST /api/chat com JSON inválido retorna 400")
    void chatWithInvalidJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    @DisplayName("chat bem-sucedido persiste mensagens USER e ASSISTANT")
    void successfulChatPersistsMessages() throws Exception {
        Session session = sessionRepository.save(new Session("Nova sessão"));

        String json = """
                {
                    "sessionId": %d,
                    "content": "Olá"
                }
                """.formatted(session.getId());

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        var messages = messageRepository.findBySession_IdOrderByTimestampAsc(session.getId());
        assertEquals(2, messages.size());
        assertEquals("USER", messages.get(0).getRole().name());
        assertEquals("ASSISTANT", messages.get(1).getRole().name());
    }

    @Test
    @DisplayName("GET /api/health retorna 200")
    void healthReturns200() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }
}
