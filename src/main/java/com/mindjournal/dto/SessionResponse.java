package com.mindjournal.dto;

import java.time.Instant;

public record SessionResponse(
    Long id,
    String title,
    Instant createdAt,
    Instant updatedAt
) {
}