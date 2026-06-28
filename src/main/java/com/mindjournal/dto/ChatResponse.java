package com.mindjournal.dto;

import java.util.List;

public record ChatResponse(
    MessageResponse userMessage,
    MessageResponse assistantMessage,
    List<SourceDTO> sources
) {}