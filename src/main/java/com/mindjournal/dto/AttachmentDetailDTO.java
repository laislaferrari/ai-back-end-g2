package com.mindjournal.dto;

import com.mindjournal.entity.AttachmentType;
import com.mindjournal.entity.DocumentStatus;
import java.time.Instant;

public record AttachmentDetailDTO(
    Long id,
    Long sessionId,
    String filename,
    AttachmentType type,
    Long size,
    Instant uploadDate,
    Long documentId,
    DocumentStatus documentStatus,
    String errorMessage
) {}
