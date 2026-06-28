CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS vector_spike_entity;

CREATE TABLE vector_spike_entity (
    id BIGSERIAL PRIMARY KEY,
    embedding vector(768) NOT NULL
);
