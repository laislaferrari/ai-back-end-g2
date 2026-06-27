package com.mindjournal.service;

import com.mindjournal.dto.MessageResponse;
import com.mindjournal.entity.Message;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.repository.MessageRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    // Construtor manual no lugar do Lombok
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageResponse createAndSaveMessage(Session session, MessageRole role, String content) {
        Message message = new Message();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setTimestamp(Instant.now());

        Message savedMessage = messageRepository.save(message);

        // Ordem exata que a Laís colocou no MessageResponse: (Long, String, MessageRole, Instant)
        return new MessageResponse(
                savedMessage.getId(),
                savedMessage.getContent(),
                savedMessage.getRole(),
                savedMessage.getTimestamp()
        );
    }
}