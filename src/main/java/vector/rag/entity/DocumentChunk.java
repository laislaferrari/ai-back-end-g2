package vector.rag.entity;

import com.mindjournal.entity.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(
    name = "document_chunks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_document_chunk_index",
        columnNames = {"document_id", "chunk_index"}
    )
)
public class DocumentChunk {

    public static final int EMBEDDING_DIMENSION = 768;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSION)
    @Column(name = "embedding", nullable = false)
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentChunk() {
    }

    public DocumentChunk(
        Document document,
        String content,
        Integer chunkIndex,
        float[] embedding
    ) {
        setDocument(document);
        setContent(content);
        setChunkIndex(chunkIndex);
        setEmbedding(embedding);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = Objects.requireNonNull(document, "document não pode ser nulo");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content não pode ser vazio");
        }

        this.content = content;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        if (chunkIndex == null || chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex deve ser maior ou igual a zero");
        }

        this.chunkIndex = chunkIndex;
    }

    public float[] getEmbedding() {
        return embedding == null
            ? null
            : Arrays.copyOf(embedding, embedding.length);
    }

    public void setEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException(
                "embedding deve possuir exatamente " + EMBEDDING_DIMENSION + " posições"
            );
        }

        this.embedding = Arrays.copyOf(embedding, embedding.length);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
