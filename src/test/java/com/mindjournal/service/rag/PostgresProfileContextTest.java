package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class PostgresProfileContextTest {

    @Autowired(required = false)
    private DocumentIngestionService ingestionService;

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("contexto carrega com profile test")
    void contextLoads() {
        assertNotNull(ingestionService,
            "DocumentIngestionService deve estar presente");
        assertNotNull(embeddingService,
            "EmbeddingService deve estar disponível");
    }
}
