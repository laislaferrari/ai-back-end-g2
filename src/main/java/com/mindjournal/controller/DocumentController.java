package com.mindjournal.controller;

import com.mindjournal.dto.DocumentStatusResponse;
import com.mindjournal.service.DocumentStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentStatusService documentStatusService;

    public DocumentController(DocumentStatusService documentStatusService) {
        this.documentStatusService = documentStatusService;
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id) {
        DocumentStatusResponse response = documentStatusService.getStatus(id);
        return ResponseEntity.ok(response);
    }
}
