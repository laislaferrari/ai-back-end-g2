package com.mindjournal.controller;

import com.mindjournal.dto.CreateSessionRequest;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SessionResponse;
import com.mindjournal.dto.UpdateTitleRequest;
import com.mindjournal.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
        @Valid @RequestBody CreateSessionRequest request
    ) {
        SessionResponse session = sessionService.createSession(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(session);
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions() {
        return ResponseEntity.ok(sessionService.listSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessagesBySessionId(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(
            sessionService.getMessagesBySessionId(id)
        );
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<SessionResponse> updateTitle(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTitleRequest request
    ) {
        SessionResponse session = sessionService.updateTitle(id, request.title());
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
        @PathVariable Long id
    ) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
