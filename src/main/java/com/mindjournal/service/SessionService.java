package com.mindjournal.service;

import com.mindjournal.dto.CreateSessionRequest;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SessionResponse;
import com.mindjournal.entity.Message;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public SessionService(
        SessionRepository sessionRepository,
        MessageRepository messageRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        Session session = new Session(request.title().trim());
        Session savedSession = sessionRepository.save(session);

        return toSessionResponse(savedSession);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions() {
        return sessionRepository
            .findAllByOrderByUpdatedAtDesc()
            .stream()
            .map(this::toSessionResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long id) {
        Session session = findSession(id);

        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesBySessionId(Long sessionId) {
        findSession(sessionId);

        return messageRepository
            .findBySession_IdOrderByTimestampAsc(sessionId)
            .stream()
            .map(this::toMessageResponse)
            .toList();
    }

    private Session findSession(Long id) {
        return sessionRepository
            .findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));
    }

    private SessionResponse toSessionResponse(Session session) {
        return new SessionResponse(
            session.getId(),
            session.getTitle(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getContent(),
            message.getRole(),
            message.getTimestamp()
        );
    }
}