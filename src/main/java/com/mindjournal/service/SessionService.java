package com.mindjournal.service;

import com.mindjournal.dto.CreateSessionRequest;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SessionResponse;
import com.mindjournal.entity.Attachment;
import com.mindjournal.entity.Message;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.AttachmentRepository;
import com.mindjournal.repository.DocumentRepository;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final DocumentRepository documentRepository;

    public SessionService(
        SessionRepository sessionRepository,
        MessageRepository messageRepository,
        AttachmentRepository attachmentRepository,
        DocumentRepository documentRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.documentRepository = documentRepository;
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

    @Transactional
    public SessionResponse updateTitle(Long sessionId, String newTitle) {
        Session session = findSession(sessionId);
        session.setTitle(newTitle.trim());
        session.setUpdatedAt(Instant.now());
        Session saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));

        List<Attachment> attachments = attachmentRepository.findBySessionId(id);
        for (Attachment attachment : attachments) {
            String filePath = attachment.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                try {
                    Files.deleteIfExists(Path.of(filePath));
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Erro ao excluir arquivo físico: " + filePath, e);
                }
            }
            documentRepository.findByAttachmentId(attachment.getId())
                .ifPresent(documentRepository::delete);
            attachmentRepository.delete(attachment);
        }

        messageRepository.deleteBySession_Id(id);

        sessionRepository.delete(session);
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