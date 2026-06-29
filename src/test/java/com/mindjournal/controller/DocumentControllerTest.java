package com.mindjournal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindjournal.dto.DocumentStatusResponse;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.service.DocumentStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentStatusService documentStatusService;

    @Test
    @DisplayName("GET /api/documents/{id}/status retorna 200 com dados do documento")
    void getStatusReturns200() throws Exception {
        DocumentStatusResponse response = new DocumentStatusResponse(
            1L, "relatorio.pdf", DocumentStatus.RECEIVED, Instant.parse("2025-01-15T10:30:00Z")
        );

        when(documentStatusService.getStatus(1L)).thenReturn(response);

        mockMvc.perform(get("/api/documents/1/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.documentId").value(1))
            .andExpect(jsonPath("$.fileName").value("relatorio.pdf"))
            .andExpect(jsonPath("$.status").value("RECEIVED"))
            .andExpect(jsonPath("$.updatedAt").value("2025-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/status retorna 404 quando serviço lança exceção")
    void getStatusReturns404() throws Exception {
        when(documentStatusService.getStatus(99L))
            .thenThrow(new com.mindjournal.exception.DocumentNotFoundException(99L));

        mockMvc.perform(get("/api/documents/99/status"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Documento não encontrado"));
    }
}
