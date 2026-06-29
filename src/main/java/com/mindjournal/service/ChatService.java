package com.mindjournal.service;

import com.mindjournal.dto.ChatRequest;
import com.mindjournal.dto.ChatResponse;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.RagService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String SYSTEM_INSTRUCTION =
            "Você é um assistente pessoal de diário, ajudando o usuário a refletir " +
            "sobre seus pensamentos e sentimentos com empatia e respeito.";

    private final SessionRepository sessionRepository;
    private final MessageService messageService;
    private final AiResponseGenerator aiResponseGenerator;
    private final ObjectProvider<RagService> ragServiceProvider;

    public ChatService(
            SessionRepository sessionRepository,
            MessageService messageService,
            AiResponseGenerator aiResponseGenerator,
            ObjectProvider<RagService> ragServiceProvider
    ) {
        this.sessionRepository = sessionRepository;
        this.messageService = messageService;
        this.aiResponseGenerator = aiResponseGenerator;
        this.ragServiceProvider = ragServiceProvider;
    }

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new SessionNotFoundException(request.sessionId()));

        MessageResponse userMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.USER,
                request.content());

        RagService ragService = ragServiceProvider.getIfAvailable();
        String context;
        List<SourceDTO> sources;

        if (ragService != null) {
            RagService.RagContext ragContext = ragService.retrieveContext(request.content());
            sources = ragContext.sources();
            context = buildContext(ragContext.content(), request.content());
        } else {
            sources = Collections.emptyList();
            context = request.content();
        }

        String aiContent = aiResponseGenerator.generateResponse(
                session.getId(), request.content(), context
        );

        MessageResponse assistantMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.ASSISTANT,
                aiContent);

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return new ChatResponse(userMsg, assistantMsg, sources);
    }

    private String buildContext(String retrievedContent, String userMessage) {
        if (retrievedContent == null || retrievedContent.isBlank()) {
            return userMessage;
        }

        return SYSTEM_INSTRUCTION
                + "\n\nContexto dos documentos do usuário:\n"
                + retrievedContent
                + "\n\nPergunta do usuário: "
                + userMessage;
    }
}