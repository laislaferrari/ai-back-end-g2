package com.mindjournal.service;

import com.mindjournal.service.rag.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AsyncIngestionService.class);

    private final ObjectProvider<DocumentIngestionService> ingestionServiceProvider;

    public AsyncIngestionService(ObjectProvider<DocumentIngestionService> ingestionServiceProvider) {
        this.ingestionServiceProvider = ingestionServiceProvider;
    }

    @Async
    public void processIngestion(Long documentId) {
        DocumentIngestionService ingestionService = ingestionServiceProvider.getIfAvailable();
        if (ingestionService == null) {
            log.info("Nenhum DocumentIngestionService disponível (profile H2). ");
            return;
        }
        try {
            ingestionService.ingest(documentId);
        } catch (Exception e) {
            log.error("Falha na ingestão do documento {}: {}", documentId, e.getMessage(), e);
        }
    }
}
