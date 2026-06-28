package com.mindjournal.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Não foi encontrado um documento com o ID " + id + ".");
    }
}
