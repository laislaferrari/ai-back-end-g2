package com.mindjournal.repository;

import com.mindjournal.entity.Message;
import com.mindjournal.entity.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySession_IdOrderByTimestampAsc(Long sessionId);

    long countBySession_IdAndRole(Long sessionId, MessageRole role);

    void deleteBySession_Id(Long sessionId);
}