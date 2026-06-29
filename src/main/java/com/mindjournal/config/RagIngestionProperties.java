package com.mindjournal.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagIngestionProperties {

    private Chunk chunk = new Chunk();
    private Embedding embedding = new Embedding();

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    @PostConstruct
    public void validate() {
        if (chunk.maxSize <= 0) {
            throw new IllegalStateException(
                "rag.chunk.max-size deve ser maior que zero, mas foi " + chunk.maxSize
            );
        }
        if (chunk.overlap < 0) {
            throw new IllegalStateException(
                "rag.chunk.overlap não pode ser negativo, mas foi " + chunk.overlap
            );
        }
        if (chunk.overlap >= chunk.maxSize) {
            throw new IllegalStateException(
                "rag.chunk.overlap (" + chunk.overlap
                    + ") deve ser menor que rag.chunk.max-size (" + chunk.maxSize + ")"
            );
        }
        if (embedding.dimension != 768) {
            throw new IllegalStateException(
                "rag.embedding.dimension deve ser 768, mas foi " + embedding.dimension
            );
        }
        if (retrieval.topK <= 0) {
            throw new IllegalStateException(
                "rag.retrieval.top-k deve ser maior que zero, mas foi " + retrieval.topK
            );
        }
        if (retrieval.minSimilarity < 0.0 || retrieval.minSimilarity > 1.0) {
            throw new IllegalStateException(
                "rag.retrieval.min-similarity deve estar entre 0.0 e 1.0, mas foi " + retrieval.minSimilarity
            );
        }
    }

    public static class Chunk {
        private int maxSize = 1000;
        private int overlap = 200;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }
    }

    public static class Embedding {
        private int dimension = 768;

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }
    }

    public static class Retrieval {
        private int topK = 3;
        private double minSimilarity = 0.70;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getMinSimilarity() {
            return minSimilarity;
        }

        public void setMinSimilarity(double minSimilarity) {
            this.minSimilarity = minSimilarity;
        }
    }

    private Retrieval retrieval = new Retrieval();

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    private String ollamaUrl = "http://localhost:11434/api/embed";
    private String ollamaModel = "embeddinggemma:300m";

    public String getOllamaUrl() { return ollamaUrl; }
    public void setOllamaUrl(String ollamaUrl) { this.ollamaUrl = ollamaUrl; }

    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }
}
