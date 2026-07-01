package com.mindjournal.service;

import com.mindjournal.dto.ChatRequest;
import com.mindjournal.dto.ChatResponse;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.RagContext;
import com.mindjournal.service.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final SessionRepository sessionRepository;
    private final MessageService messageService;
    private final AiResponseGenerator aiResponseGenerator;
    private final ObjectProvider<RagService> ragServiceProvider;
    private final MessageRepository messageRepository;
    private final TitleGeneratorService titleGeneratorService;

    public ChatService(SessionRepository sessionRepository,
                       MessageService messageService,
                       AiResponseGenerator aiResponseGenerator,
                       ObjectProvider<RagService> ragServiceProvider,
                       MessageRepository messageRepository,
                       TitleGeneratorService titleGeneratorService) {
        this.sessionRepository = sessionRepository;
        this.messageService = messageService;
        this.aiResponseGenerator = aiResponseGenerator;
        this.ragServiceProvider = ragServiceProvider;
        this.messageRepository = messageRepository;
        this.titleGeneratorService = titleGeneratorService;
    }

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new SessionNotFoundException(request.sessionId()));

        boolean isFirstUserMessage = messageRepository
                .countBySession_IdAndRole(session.getId(), MessageRole.USER) == 0;

        MessageResponse userMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.USER,
                request.content());

        if (isFirstUserMessage) {
            generateAndSetTitle(session, request.content());
        }

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

        return new ChatResponse(userMsg, assistantMsg, sources, session.getTitle());
    }

    private void generateAndSetTitle(Session session, String content) {
        try {
            String title = titleGeneratorService.generateTitle(content);
            session.setTitle(title);
        } catch (Exception e) {
            log.warn("Falha ao gerar título automático, o chat continuará normalmente.", e);
        }
    }
}