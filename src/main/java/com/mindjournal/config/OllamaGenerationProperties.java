package com.mindjournal.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.generation")
public class OllamaGenerationProperties {

    private String ollamaUrl = "http://localhost:11434/api/chat";
    private String model = "llama3.2:3b";
    private double temperature = 0.2;

    public String getOllamaUrl() {
        return ollamaUrl;
    }

    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @PostConstruct
    public void validate() {
        if (ollamaUrl == null || ollamaUrl.isBlank()) {
            throw new IllegalStateException("rag.generation.ollama-url não pode ser vazio.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("rag.generation.model não pode ser vazio.");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalStateException(
                "rag.generation.temperature deve estar entre 0.0 e 2.0, mas foi " + temperature
            );
        }
    }
}
