package com.mindjournal.service.parsing;

public interface DocumentParser {

    String parse(byte[] content, String contentType);
}
