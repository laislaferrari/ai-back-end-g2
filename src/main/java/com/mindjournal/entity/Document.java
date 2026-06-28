package com.mindjournal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false, unique = true)
    private Attachment attachment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Document() {
    }

    public Document(Attachment attachment) {
        this.attachment = Objects.requireNonNull(attachment, "attachment não pode ser nulo");
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (status == null) {
            status = DocumentStatus.RECEIVED;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markAsProcessing() {
        requireStatus(DocumentStatus.RECEIVED);
        status = DocumentStatus.PROCESSING;
        errorMessage = null;
    }

    public void markAsIndexed() {
        requireStatus(DocumentStatus.PROCESSING);
        status = DocumentStatus.INDEXED;
        errorMessage = null;
    }

    public void markAsFailed(String message) {
        requireStatus(DocumentStatus.PROCESSING);

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("mensagem de erro não pode ser vazia");
        }

        status = DocumentStatus.FAILED;
        errorMessage = message;
    }

    public void resetForReprocessing() {
        if (status != DocumentStatus.FAILED && status != DocumentStatus.INDEXED) {
            throw new IllegalStateException(
                "somente documentos FAILED ou INDEXED podem ser reprocessados"
            );
        }

        status = DocumentStatus.RECEIVED;
        errorMessage = null;
    }

    private void requireStatus(DocumentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                "transição inválida: esperado " + expected + ", atual " + status
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = Objects.requireNonNull(attachment, "attachment não pode ser nulo");
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
