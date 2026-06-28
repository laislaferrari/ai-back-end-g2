package com.mindjournal.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("postgres")
@EntityScan(basePackages = {
    "com.mindjournal.entity",
    "vector.rag.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.mindjournal.repository",
    "vector.rag.repository"
})
public class RagPersistenceConfig {
}
