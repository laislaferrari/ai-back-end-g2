package com.mindjournal.controller;

import com.mindjournal.dto.AttachmentDTO;
import com.mindjournal.dto.AttachmentInput;
import com.mindjournal.exception.InvalidFileException;
import com.mindjournal.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class AttachmentController {

    private final AttachmentService attachmentService;

    // Construtor manual (Sem Lombok)
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<AttachmentDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") Long sessionId) {

        if (file.isEmpty()) {
            throw new InvalidFileException("O arquivo não pode estar vazio.");
        }

        try {
            // Converte o MultipartFile em uma entrada neutra para o Service
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
}
