package com.mindjournal.service;

import com.mindjournal.dto.ChatRequest;
import com.mindjournal.dto.ChatResponse;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class ChatService {

    private final SessionRepository sessionRepository;
    private final MessageService messageService;
    private final AiResponseGenerator aiResponseGenerator;

    // Construtor manual no lugar do Lombok
    public ChatService(SessionRepository sessionRepository, MessageService messageService,
            AiResponseGenerator aiResponseGenerator) {
        this.sessionRepository = sessionRepository;
        this.messageService = messageService;
        this.aiResponseGenerator = aiResponseGenerator;
    }

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        // Agora passamos o Long (sessionId) na Exception, como a Laís implementou
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new SessionNotFoundException(request.sessionId()));

        MessageResponse userMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.USER,
                request.content());

        String aiContent = aiResponseGenerator.generateResponse(session.getId(), request.content());

        MessageResponse assistantMsg = messageService.createAndSaveMessage(
                session,
                MessageRole.ASSISTANT,
                aiContent);

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return new ChatResponse(userMsg, assistantMsg, java.util.Collections.emptyList());
    }
}