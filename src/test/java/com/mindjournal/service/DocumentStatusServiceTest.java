package com.mindjournal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mindjournal.dto.DocumentStatusResponse;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import com.mindjournal.exception.DocumentNotFoundException;
import com.mindjournal.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DocumentStatusServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    private DocumentStatusService service;

    @BeforeEach
    void setUp() {
        service = new DocumentStatusService(documentRepository);
    }

    @Test
    @DisplayName("getStatus retorna DTO com dados corretos quando documento existe")
    void getStatusReturnsDto() {
        Attachment attachment = new Attachment();
        attachment.setFilename("relatorio.pdf");

        Document document = new Document(attachment);
        ReflectionTestUtils.setField(document, "id", 1L);
        ReflectionTestUtils.setField(document, "status", DocumentStatus.INDEXED);
        ReflectionTestUtils.setField(document, "updatedAt", Instant.parse("2025-06-01T12:00:00Z"));

        when(documentRepository.findByIdWithAttachment(1L)).thenReturn(Optional.of(document));

        DocumentStatusResponse response = service.getStatus(1L);

        assertNotNull(response);
        assertEquals(1L, response.documentId());
        assertEquals("relatorio.pdf", response.fileName());
        assertEquals(DocumentStatus.INDEXED, response.status());
        assertEquals(Instant.parse("2025-06-01T12:00:00Z"), response.updatedAt());
    }

    @Test
    @DisplayName("getStatus lança DocumentNotFoundException quando documento não existe")
    void getStatusThrowsNotFound() {
        when(documentRepository.findByIdWithAttachment(99L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> service.getStatus(99L));
    }

    @Test
    @DisplayName("getStatus usa findByIdWithAttachment")
    void getStatusUsesFindByIdWithAttachment() {
        when(documentRepository.findByIdWithAttachment(anyLong()))
            .thenReturn(Optional.of(new Document(new Attachment())));

        assertDoesNotThrow(() -> service.getStatus(1L));
        verify(documentRepository).findByIdWithAttachment(1L);
    }
}
