package com.mindjournal.service;

import com.mindjournal.config.OllamaGenerationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TitleGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TitleGeneratorService.class);
    private static final int MAX_TITLE_LENGTH = 150;
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{P}\\p{S}]");

    private final RestTemplate restTemplate;
    private final OllamaGenerationProperties properties;
    private final boolean ollamaAvailable;

    public TitleGeneratorService(
        OllamaGenerationProperties properties,
        RestTemplate restTemplate
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.ollamaAvailable = true;
    }

    public TitleGeneratorService() {
        this.properties = null;
        this.restTemplate = null;
        this.ollamaAvailable = false;
    }

    public String generateTitle(String userMessage) {
        if (ollamaAvailable) {
            try {
                OllamaAiResponseGenerator.ChatRequest request = new OllamaAiResponseGenerator.ChatRequest(
                    properties.getModel(),
                    false,
                    List.of(
                        new OllamaAiResponseGenerator.ChatMessage("system",
                            "Crie um título curto, de 3 a 7 palavras, para a conversa abaixo. " +
                            "Retorne somente o título, sem aspas, sem explicações e sem pontuação final."),
                        new OllamaAiResponseGenerator.ChatMessage("user", userMessage)
                    ),
                    new OllamaAiResponseGenerator.ChatOptions(properties.getTemperature())
                );

                RequestEntity<OllamaAiResponseGenerator.ChatRequest> req = RequestEntity
                    .post(URI.create(properties.getOllamaUrl()))
                    .body(request);

                ResponseEntity<OllamaAiResponseGenerator.ChatResponse> res =
                    restTemplate.exchange(req, OllamaAiResponseGenerator.ChatResponse.class);

                OllamaAiResponseGenerator.ChatResponse body = res.getBody();
                if (body != null && body.message() != null && body.message().content() != null) {
                    String title = body.message().content()
                        .replaceAll("^\"+|\"+$", "")
                        .replaceAll("\\.$", "")
                        .trim();
                    if (!title.isEmpty() && title.length() <= MAX_TITLE_LENGTH) {
                        return title;
                    }
                }
            } catch (Exception e) {
                log.warn("Falha ao gerar título com IA, utilizando fallback.", e);
            }
        }
        return fallbackTitle(userMessage);
    }

    static String fallbackTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Nova conversa";
        }
        String cleaned = PUNCTUATION.matcher(userMessage).replaceAll(" ").trim();
        String[] words = cleaned.split("\\s+");
        if (words.length == 0) {
            return "Nova conversa";
        }
        int limit = Math.min(words.length, 7);
        if (limit < 3) {
            limit = Math.min(words.length, 3);
        }
        String title = String.join(" ", java.util.Arrays.copyOf(words, limit)).trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH).trim();
        }
        return title;
    }
}
