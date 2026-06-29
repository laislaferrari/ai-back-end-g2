package com.mindjournal.service;

import com.mindjournal.dto.ChatRequest;
import com.mindjournal.dto.ChatResponse;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.RagContext;
import com.mindjournal.service.rag.RagService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private final SessionRepository sessionRepository;
    private final MessageService messageService;
    private final AiResponseGenerator aiResponseGenerator;
    private final ObjectProvider<RagService> ragServiceProvider;

    public ChatService(SessionRepository sessionRepository,
                       MessageService messageService,
                       AiResponseGenerator aiResponseGenerator,
                       ObjectProvider<RagService> ragServiceProvider) {
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
        List<SourceDTO> sources;
        String context;
        if (ragService != null) {
            RagContext ragContext = ragService.retrieveContext(session.getId(), request.content());
            sources = ragContext.sources();
            context = ragContext.context();
        } else {
            sources = Collections.emptyList();
            context = "";
        }

        String aiContent = aiResponseGenerator.generateResponse(session.getId(), request.content(), context);

        MessageResponse assistantMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.ASSISTANT,
                aiContent);

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return new ChatResponse(userMsg, assistantMsg, sources);
    }
}