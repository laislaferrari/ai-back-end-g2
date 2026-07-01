package com.mindjournal.controller;

import com.mindjournal.dto.AttachmentDTO;
import com.mindjournal.dto.AttachmentDetailDTO;
import com.mindjournal.dto.AttachmentInput;
import com.mindjournal.exception.InvalidFileException;
import com.mindjournal.service.AttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/api/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<AttachmentDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") Long sessionId) {

        if (file.isEmpty()) {
            throw new InvalidFileException("O arquivo não pode estar vazio.");
        }

        try {
            AttachmentInput input = new AttachmentInput(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes(),
                    sessionId
            );

            AttachmentDTO response = attachmentService.uploadAttachment(input);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar a leitura do arquivo.", e);
        }
    }

    @GetMapping("/api/sessions/{sessionId}/attachments")
    public ResponseEntity<List<AttachmentDetailDTO>> listAttachments(
            @PathVariable Long sessionId) {
        List<AttachmentDetailDTO> attachments = attachmentService.listAttachmentsBySession(sessionId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/api/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable Long attachmentId) {
        var fileData = attachmentService.getAttachmentFile(attachmentId);

        Path filePath = fileData.path();
        String filename = fileData.filename();
        String contentType = fileData.contentType();

        try {
            var resource = new InputStreamResource(new FileInputStream(filePath.toFile()));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(Files.size(filePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo para download.", e);
        }
    }
}
