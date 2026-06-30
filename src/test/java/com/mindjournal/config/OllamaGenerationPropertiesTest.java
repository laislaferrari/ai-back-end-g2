package com.mindjournal.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OllamaGenerationPropertiesTest {

    @Test
    @DisplayName("propriedades válidas não lançam exceção")
    void validProperties() {
        var props = new OllamaGenerationProperties();
        props.setOllamaUrl("http://localhost:11434/api/chat");
        props.setModel("llama3.2:3b");
        props.setTemperature(0.2);
        assertDoesNotThrow(props::validate);
    }

    @Test
    @DisplayName("URL vazia lança exceção")
    void emptyUrlThrowsException() {
        var props = new OllamaGenerationProperties();
        props.setOllamaUrl("");
        props.setModel("llama3.2:3b");
        props.setTemperature(0.2);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("modelo vazio lança exceção")
    void emptyModelThrowsException() {
        var props = new OllamaGenerationProperties();
        props.setOllamaUrl("http://localhost:11434/api/chat");
        props.setModel("");
        props.setTemperature(0.2);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("temperatura abaixo de 0.0 lança exceção")
    void temperatureBelowZeroThrowsException() {
        var props = new OllamaGenerationProperties();
        props.setOllamaUrl("http://localhost:11434/api/chat");
        props.setModel("llama3.2:3b");
        props.setTemperature(-0.1);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("temperatura acima de 2.0 lança exceção")
    void temperatureAboveTwoThrowsException() {
        var props = new OllamaGenerationProperties();
        props.setOllamaUrl("http://localhost:11434/api/chat");
        props.setModel("llama3.2:3b");
        props.setTemperature(2.1);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("defaults são seguros")
    void safeDefaults() {
        var props = new OllamaGenerationProperties();
        assertEquals("http://localhost:11434/api/chat", props.getOllamaUrl());
        assertEquals("llama3.2:3b", props.getModel());
        assertEquals(0.2, props.getTemperature(), 0.0001);
    }
}
