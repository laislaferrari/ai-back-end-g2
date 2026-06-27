package com.mindjournal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotNull(message = "O sessionId é obrigatório") 
        Long sessionId,
        
        @NotBlank(message = "O conteúdo da mensagem não pode ser vazio") 
        String content
) {}