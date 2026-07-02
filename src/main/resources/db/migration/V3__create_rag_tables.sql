CREATE TABLE IF NOT EXISTS documents (
    id            BIGSERIAL PRIMARY KEY,
    attachment_id BIGINT NOT NULL UNIQUE,
    status        VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_documents_attachment
        FOREIGN KEY (attachment_id)
        REFERENCES attachments(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_documents_status
        CHECK (status IN ('RECEIVED', 'PROCESSING', 'INDEXED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content     TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding   TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_document_chunk_index
        UNIQUE (document_id, chunk_index),
    CONSTRAINT chk_document_chunks_index
        CHECK (chunk_index >= 0),
    CONSTRAINT chk_document_chunks_content
        CHECK (length(trim(content)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_documents_status
    ON documents(status);

CREATE INDEX IF NOT EXISTS idx_documents_attachment_id
    ON documents(attachment_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
    ON document_chunks(document_id);
