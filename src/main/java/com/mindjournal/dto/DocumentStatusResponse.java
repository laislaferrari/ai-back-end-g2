package com.mindjournal.dto;

import com.mindjournal.entity.DocumentStatus;
import java.time.Instant;

public record DocumentStatusResponse(
    Long documentId,
    String fileName,
    DocumentStatus status,
    Instant updatedAt
) {
}
