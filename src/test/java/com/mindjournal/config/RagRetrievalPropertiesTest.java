package com.mindjournal.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RagRetrievalPropertiesTest {

    @Test
    @DisplayName("propriedades válidas não lançam exceção")
    void validProperties() {
        var props = new RagRetrievalProperties();
        props.setTopK(3);
        props.setMinSimilarity(0.70);
        assertDoesNotThrow(props::validate);
    }

    @Test
    @DisplayName("topK menor ou igual a zero lança exceção")
    void topKMustBePositive() {
        var props = new RagRetrievalProperties();
        props.setTopK(0);
        props.setMinSimilarity(0.70);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("minSimilarity abaixo de zero lança exceção")
    void minSimilarityMustNotBeNegative() {
        var props = new RagRetrievalProperties();
        props.setTopK(3);
        props.setMinSimilarity(-0.1);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("minSimilarity acima de um lança exceção")
    void minSimilarityMustNotExceedOne() {
        var props = new RagRetrievalProperties();
        props.setTopK(3);
        props.setMinSimilarity(1.1);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("defaults são seguros")
    void safeDefaults() {
        var props = new RagRetrievalProperties();
        assertEquals(3, props.getTopK());
        assertEquals(0.70, props.getMinSimilarity(), 0.0001);
    }
}
