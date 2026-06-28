package com.mindjournal.dto;

public record SourceDTO(
    Long documentId, 
    String fileName, 
    Long chunkId, 
    String content, 
    double similarityScore
) {}
