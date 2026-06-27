package com.mindjournal.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(Long id) {
        super("Não foi encontrada uma sessão com o ID " + id + ".");
    }
}