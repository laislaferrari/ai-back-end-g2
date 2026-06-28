package com.mindjournal.service.rag;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.service.embedding.EmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("postgres")
@Testcontainers
class PostgresProfileContextTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired(required = false)
    private DocumentIngestionService ingestionService;

    @Autowired
    private ObjectProvider<EmbeddingService> embeddingServiceProvider;

    @Test
    @DisplayName("contexto carrega com profile postgres sem MemoryEmbeddingService")
    void contextLoadsWithPostgresProfile() {
        assertNotNull(ingestionService,
            "DocumentIngestionService deve estar presente no profile postgres");
        assertNull(embeddingServiceProvider.getIfAvailable(),
            "EmbeddingService não deve estar disponível sem o profile test");
    }
}
