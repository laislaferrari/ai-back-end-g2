package com.mindjournal.spike;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import vector.spike.VectorSpikeEntity;
import vector.spike.VectorSpikeRepository;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories(basePackages = {"com.mindjournal.spike", "vector.spike"})
@EntityScan(basePackages = {"com.mindjournal.spike", "vector.spike"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PgVectorSpikeTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.database-platform",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private VectorSpikeRepository repository;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void createTable() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("spike/create-vector-table.sql"));
        }
    }

    private static float[] vector(float... values) {
        float[] result = new float[768];
        for (int i = 0; i < values.length && i < 768; i++) {
            result[i] = values[i];
        }
        return result;
    }

    @Test
    @Order(1)
    @DisplayName("vector extension is enabled")
    void vectorExtensionIsEnabled() {
        String ext = jdbc.queryForObject(
            "SELECT extname FROM pg_extension WHERE extname = 'vector'", String.class);
        assertEquals("vector", ext);
    }

    @Test
    @Order(2)
    @DisplayName("column type is vector(768)")
    void columnTypeIsVector() {
        String udt = jdbc.queryForObject(
            "SELECT udt_name FROM information_schema.columns " +
            "WHERE table_name = 'vector_spike_entity' AND column_name = 'embedding'",
            String.class);
        assertEquals("vector", udt);
    }

    @Test
    @Order(3)
    @DisplayName("persists and reads vector via JPA with 768 positions fidelity")
    void persistAndRead() {
        float[] embedding = vector(1.0f);
        var entity = new VectorSpikeEntity(embedding);
        entity = repository.saveAndFlush(entity);
        assertNotNull(entity.getId());

        em.clear();

        var loaded = repository.findById(entity.getId()).orElseThrow();
        assertEquals(768, loaded.getEmbedding().length);
        assertArrayEquals(embedding, loaded.getEmbedding(), 0.0001f);
    }

    @Test
    @Order(4)
    @DisplayName("updates vector via JPA")
    void updateVector() {
        float[] original = vector(1.0f);
        var entity = repository.saveAndFlush(new VectorSpikeEntity(original));
        em.clear();

        float[] updated = vector(0.5f, 0.5f);
        var loaded = repository.findById(entity.getId()).orElseThrow();
        loaded.setEmbedding(updated);
        repository.flush();
        em.clear();

        var reloaded = repository.findById(entity.getId()).orElseThrow();
        assertArrayEquals(updated, reloaded.getEmbedding(), 0.0001f);
    }

    @Test
    @Order(5)
    @DisplayName("rejects wrong vector dimension on flush")
    void wrongDimensionIsRejected() {
        float[] wrong = new float[767];
        wrong[0] = 1.0f;

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(new VectorSpikeEntity(wrong));
        });

        em.clear();
    }

    @Test
    @Order(6)
    @DisplayName("queries with cosine_distance HQL and float[] parameter")
    void hqlCosineDistance() {
        float[] near = vector(1.0f);
        float[] mid = vector(0.8f, 0.6f);
        float[] far = vector(0.0f, 1.0f);

        repository.saveAllAndFlush(Arrays.asList(
            new VectorSpikeEntity(near),
            new VectorSpikeEntity(mid),
            new VectorSpikeEntity(far)
        ));
        em.clear();

        float[] query = vector(1.0f);

        List<Object[]> results = em.createQuery(
            "SELECT e.id, cosine_distance(e.embedding, :query) " +
            "FROM VectorSpikeEntity e " +
            "ORDER BY cosine_distance(e.embedding, :query)", Object[].class)
            .setParameter("query", query)
            .setMaxResults(2)
            .getResultList();

        assertEquals(2, results.size());

        double dist0 = ((Number) results.get(0)[1]).doubleValue();
        double dist1 = ((Number) results.get(1)[1]).doubleValue();

        assertTrue(dist0 <= dist1, "first result should have smaller distance");
        assertTrue(dist0 < 0.01, "near vector distance ~0, got " + dist0);
        assertTrue(dist1 > 0.1, "mid vector distance ~0.2, got " + dist1);
    }

    @Test
    @Order(7)
    @DisplayName("queries with native <=> operator and float[] parameter")
    void nativeCosineDistance() {
        float[] near = vector(1.0f);
        float[] far = vector(0.0f, 1.0f);
        repository.saveAllAndFlush(Arrays.asList(
            new VectorSpikeEntity(near),
            new VectorSpikeEntity(far)
        ));
        em.clear();

        List<Object[]> results = em.createNativeQuery(
            "SELECT e.id, " +
            "  1 - (e.embedding <=> CAST(:query AS vector(768))) AS score " +
            "FROM vector_spike_entity e " +
            "WHERE 1 - (e.embedding <=> CAST(:query AS vector(768))) >= :minSim " +
            "ORDER BY e.embedding <=> CAST(:query AS vector(768)) " +
            "LIMIT :topK")
            .setParameter("query", vector(1.0f))
            .setParameter("minSim", 0.5)
            .setParameter("topK", 1)
            .getResultList();

        assertEquals(1, results.size(), "topK should limit results");
        double score = ((Number) results.get(0)[1]).doubleValue();
        assertTrue(score > 0.99, "near vector should have near-perfect score, got " + score);
    }

    @Test
    @Order(8)
    @DisplayName("filters by minSimilarity")
    void minSimilarityFilter() {
        float[] near = vector(1.0f);
        float[] far = vector(0.0f, 1.0f);
        repository.saveAllAndFlush(Arrays.asList(
            new VectorSpikeEntity(near),
            new VectorSpikeEntity(far)
        ));
        em.clear();

        List<Object[]> results = em.createNativeQuery(
            "SELECT e.id, " +
            "  1 - (e.embedding <=> CAST(:query AS vector(768))) AS score " +
            "FROM vector_spike_entity e " +
            "WHERE 1 - (e.embedding <=> CAST(:query AS vector(768))) >= :minSim " +
            "ORDER BY e.embedding <=> CAST(:query AS vector(768)) " +
            "LIMIT :topK")
            .setParameter("query", vector(1.0f))
            .setParameter("minSim", 0.01)
            .setParameter("topK", 10)
            .getResultList();

        assertFalse(results.isEmpty());
        for (Object[] row : results) {
            double score = ((Number) row[1]).doubleValue();
            assertTrue(score > 0.5, "score " + score + " should be high for a near match");
        }
    }
}
