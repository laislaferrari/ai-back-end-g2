package com.mindjournal.service.rag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.Document;
import com.mindjournal.entity.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class N8nWebhookDocumentIndexingNotifierTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Attachment attachment;

    private N8nWebhookDocumentIndexingNotifier notifier;

    private Document document;

    @BeforeEach
    void setUp() {
        document = mock(Document.class);
    }

    @Test
    @DisplayName("URL vazia não envia requisição HTTP")
    void blankUrlDoesNotSendRequest() {
        notifier = new N8nWebhookDocumentIndexingNotifier(restTemplate, "");

        notifier.notifyIndexed(document, 5);

        verify(restTemplate, never()).postForEntity(any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("URL nula não envia requisição HTTP")
    void nullUrlDoesNotSendRequest() {
        notifier = new N8nWebhookDocumentIndexingNotifier(restTemplate, null);

        notifier.notifyIndexed(document, 5);

        verify(restTemplate, never()).postForEntity(any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("URL configurada envia requisição HTTP")
    void configuredUrlSendsRequest() {
        when(document.getAttachment()).thenReturn(attachment);
        when(attachment.getFilename()).thenReturn("relatorio.pdf");
        when(document.getStatus()).thenReturn(DocumentStatus.INDEXED);

        notifier = new N8nWebhookDocumentIndexingNotifier(restTemplate, "http://localhost:5678/webhook/test");

        notifier.notifyIndexed(document, 3);

        verify(restTemplate).postForEntity(
            eq("http://localhost:5678/webhook/test"),
            any(),
            eq(String.class)
        );
    }
}
