package com.mindjournal.repository;

import com.mindjournal.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByOrderByUpdatedAtDesc();
}