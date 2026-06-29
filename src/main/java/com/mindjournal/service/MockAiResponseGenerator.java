package com.mindjournal.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!postgres | test")
public class MockAiResponseGenerator implements AiResponseGenerator {

    @Override
    public String generateResponse(Long sessionId, String userMessage, String context) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Poderia reformular? Não entendi muito bem.";
        }

        String lowerContent = userMessage.toLowerCase();

        if (lowerContent.contains("triste") || lowerContent.contains("dif\u00edcil") || lowerContent.contains("mal")) {
            return "Sinto muito que voc\u00ea esteja passando por isso. Lembre-se que dias dif\u00edceis passam. Quer falar mais sobre isso?";
        } else if (lowerContent.contains("feliz") || lowerContent.contains("\u00f3timo") || lowerContent.contains("consegui")) {
            return "Que not\u00edcia excelente! \u00c9 muito bom celebrar essas conquistas. Como voc\u00ea se sente agora?";
        }

        return "Compreendo. Me conte mais sobre isso, como isso faz voc\u00ea se sentir?";
    }
}
