package com.mindjournal.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

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

    @PostConstruct
    public void validate() {
        if (topK <= 0) {
            throw new IllegalStateException(
                "rag.retrieval.top-k deve ser maior que zero, mas foi " + topK
            );
        }
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalStateException(
                "rag.retrieval.min-similarity deve estar entre 0.0 e 1.0, mas foi " + minSimilarity
            );
        }
    }
}
