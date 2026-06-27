package com.mindjournal.dto;

public record ChatResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage
) {}