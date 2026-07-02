package com.mindjournal.service;

import com.mindjournal.service.rag.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AsyncIngestionService.class);

    private final DocumentIngestionService ingestionService;

    public AsyncIngestionService(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Async
    public void processIngestion(Long documentId) {
        try {
            log.info("Iniciando ingestão assíncrona do documento {}", documentId);
            ingestionService.ingest(documentId);
            log.info("Ingestão concluída para o documento {}", documentId);
        } catch (Exception e) {
            log.error("Falha na ingestão do documento {}: {}", documentId, e.getMessage(), e);
        }
    }
}
