CREATE TABLE IF NOT EXISTS journal_sessions (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id         BIGSERIAL    PRIMARY KEY,
    content    TEXT         NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
    session_id BIGINT       NOT NULL,
    CONSTRAINT fk_messages_session
        FOREIGN KEY (session_id)
        REFERENCES journal_sessions(id)
);

CREATE TABLE IF NOT EXISTS attachments (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL,
    filename    VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    size        BIGINT       NOT NULL,
    file_path   VARCHAR(255) NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_attachments_session
        FOREIGN KEY (session_id)
        REFERENCES journal_sessions(id)
);

CREATE INDEX IF NOT EXISTS idx_messages_session_id
    ON messages(session_id);

CREATE INDEX IF NOT EXISTS idx_attachments_session_id
    ON attachments(session_id);
