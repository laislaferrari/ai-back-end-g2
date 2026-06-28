package com.mindjournal.service.rag;

public record IngestionSource(
    Long documentId,
    String filePath,
    String contentType
) {
}
