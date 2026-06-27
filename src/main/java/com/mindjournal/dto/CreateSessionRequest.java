package com.mindjournal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(

    @NotBlank(message = "O título da sessão é obrigatório.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    String title

) {
}