package com.mindjournal.repository;

public record RelevantChunkProjection(
    Long documentId,
    String fileName,
    Long chunkId,
    String content,
    Double similarityScore
) {}
