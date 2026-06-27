package com.mindjournal.dto;

public record AttachmentInput(
        String originalFilename,
        String mimeType,
        long size,
        byte[] content,
        Long sessionId
) {}