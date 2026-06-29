package com.mindjournal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mindjournal.config.OllamaGenerationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class ProfileAiResponseGeneratorTest {

    @Autowired
    private ObjectProvider<AiResponseGenerator> generatorProvider;

    @Test
    @DisplayName("profile test cria somente MockAiResponseGenerator")
    void testProfileHasOnlyMock() {
        AiResponseGenerator generator = generatorProvider.getIfAvailable();
        assertNotNull(generator);
        assertInstanceOf(MockAiResponseGenerator.class, generator);
    }

    @Test
    @DisplayName("MockAiResponseGenerator tem profile correto")
    void mockHasCorrectProfile() {
        Profile profile = MockAiResponseGenerator.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertTrue(List.of(profile.value()).contains("!postgres | test"),
            "MockAiResponseGenerator deve ter profile !postgres | test");
    }

    @Test
    @DisplayName("OllamaAiResponseGenerator tem profile correto")
    void ollamaHasCorrectProfile() {
        Profile profile = OllamaAiResponseGenerator.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertTrue(List.of(profile.value()).contains("postgres & !test"),
            "OllamaAiResponseGenerator deve ter profile postgres & !test");
    }
}
