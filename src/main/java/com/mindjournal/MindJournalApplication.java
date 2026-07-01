package com.mindjournal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MindJournalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindJournalApplication.class, args);
    }
}
