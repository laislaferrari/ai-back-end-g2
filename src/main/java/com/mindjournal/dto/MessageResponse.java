package com.mindjournal.dto;

import com.mindjournal.entity.MessageRole;

import java.time.Instant;

public record MessageResponse(
    Long id,
    String content,
    MessageRole role,
    Instant timestamp
) {
}