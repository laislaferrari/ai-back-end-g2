package com.mindjournal.service.rag;

import com.mindjournal.entity.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
@Profile("postgres")
public class N8nWebhookDocumentIndexingNotifier implements DocumentIndexingNotifier {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookDocumentIndexingNotifier.class);

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    @Autowired
    public N8nWebhookDocumentIndexingNotifier(
        @Value("${n8n.webhook.document-indexed-url:}") String webhookUrl
    ) {
        this(new RestTemplate(), webhookUrl);
    }

    N8nWebhookDocumentIndexingNotifier(RestTemplate restTemplate, String webhookUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void notifyIndexed(Document document, int indexedChunks) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("URL do webhook n8n não configurada — notificação ignorada para o documento {}", document.getId());
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                "event", "DOCUMENT_INDEXED",
                "documentId", document.getId(),
                "fileName", document.getAttachment().getFilename(),
                "status", document.getStatus().name(),
                "indexedChunks", indexedChunks,
                "timestamp", Instant.now().toString()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("Notificação enviada ao n8n para o documento {}", document.getId());
        } catch (Exception e) {
            log.warn("Falha ao notificar webhook n8n para o documento {}: {}", document.getId(), e.getMessage());
        }
    }
}
