package com.mindjournal.service;

public interface AiResponseGenerator {
    String generateResponse(Long sessionId, String userMessage, String context);
}