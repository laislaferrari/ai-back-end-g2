package com.mindjournal.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RagIngestionPropertiesTest {

    @Test
    @DisplayName("propriedades válidas não lançam exceção")
    void validProperties() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(1000);
        props.getChunk().setOverlap(200);
        props.getEmbedding().setDimension(768);
        assertDoesNotThrow(props::validate);
    }

    @Test
    @DisplayName("maxSize <= 0 lança exceção")
    void maxSizeMustBePositive() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(0);
        props.getChunk().setOverlap(0);
        props.getEmbedding().setDimension(768);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("overlap negativo lança exceção")
    void overlapMustNotBeNegative() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(-1);
        props.getEmbedding().setDimension(768);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("overlap igual a maxSize lança exceção")
    void overlapMustBeLessThanMaxSize() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(100);
        props.getEmbedding().setDimension(768);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("overlap maior que maxSize lança exceção")
    void overlapExceedsMaxSize() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(150);
        props.getEmbedding().setDimension(768);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("dimension diferente de 768 lança exceção")
    void dimensionMustBe768() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(10);
        props.getEmbedding().setDimension(512);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("defaults são seguros")
    void safeDefaults() {
        var props = new RagIngestionProperties();
        assertEquals(1000, props.getChunk().getMaxSize());
        assertEquals(200, props.getChunk().getOverlap());
        assertEquals(768, props.getEmbedding().getDimension());
        assertEquals(3, props.getRetrieval().getTopK());
        assertEquals(0.70, props.getRetrieval().getMinSimilarity(), 0.001);
    }

    @Test
    @DisplayName("topK <= 0 lança exceção")
    void topKMustBePositive() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(10);
        props.getEmbedding().setDimension(768);
        props.getRetrieval().setTopK(0);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("minSimilarity negativo lança exceção")
    void minSimilarityMustNotBeNegative() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(10);
        props.getEmbedding().setDimension(768);
        props.getRetrieval().setMinSimilarity(-0.1);
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("minSimilarity acima de 1.0 lança exceção")
    void minSimilarityMustNotExceedOne() {
        var props = new RagIngestionProperties();
        props.getChunk().setMaxSize(100);
        props.getChunk().setOverlap(10);
        props.getEmbedding().setDimension(768);
        props.getRetrieval().setMinSimilarity(1.1);
        assertThrows(IllegalStateException.class, props::validate);
    }
}
