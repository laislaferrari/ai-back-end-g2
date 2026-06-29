package com.mindjournal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    RagIngestionProperties.class,
    RagRetrievalProperties.class
})
public class RagIngestionConfig {
}
