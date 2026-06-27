package com.mindjournal.controller;

import com.mindjournal.dto.HealthDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthDTO> health() {
        return ResponseEntity.ok(new HealthDTO("UP", Instant.now()));
    }
}
