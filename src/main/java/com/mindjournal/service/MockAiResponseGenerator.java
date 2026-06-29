package com.mindjournal.service;

import org.springframework.stereotype.Service;

@Service
public class MockAiResponseGenerator implements AiResponseGenerator {

    @Override
    public String generateResponse(Long sessionId, String userMessage, String context) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Poderia reformular? Não entendi muito bem.";
        }

        String lowerContent = userMessage.toLowerCase();

        if (lowerContent.contains("triste") || lowerContent.contains("difícil") || lowerContent.contains("mal")) {
            return "Sinto muito que você esteja passando por isso. Lembre-se que dias difíceis passam. Quer falar mais sobre isso?";
        } else if (lowerContent.contains("feliz") || lowerContent.contains("ótimo") || lowerContent.contains("consegui")) {
            return "Que notícia excelente! É muito bom celebrar essas conquistas. Como você se sente agora?";
        }

        return "Compreendo. Me conte mais sobre isso, como isso faz você se sentir?";
    }
}