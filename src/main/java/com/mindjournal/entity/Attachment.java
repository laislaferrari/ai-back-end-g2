package com.mindjournal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentType type;

    @Column(nullable = false)
    private Long size;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate;

    // Construtor vazio obrigatório do JPA
    public Attachment() {}

    // Getters e Setters (Sem Lombok)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public AttachmentType getType() { return type; }
    public void setType(AttachmentType type) { this.type = type; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Instant getUploadDate() { return uploadDate; }
    public void setUploadDate(Instant uploadDate) { this.uploadDate = uploadDate; }
}