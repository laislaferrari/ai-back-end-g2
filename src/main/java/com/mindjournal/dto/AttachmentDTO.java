package com.mindjournal.dto;

import com.mindjournal.entity.AttachmentType;
import java.time.Instant;

public record AttachmentDTO(
        Long id,
        Long sessionId,
        String filename,
        AttachmentType type,
        Long size,
        Instant uploadDate
) {}