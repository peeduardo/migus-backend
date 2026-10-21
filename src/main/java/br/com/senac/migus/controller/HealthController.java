package br.com.senac.migus.controller;

import br.com.senac.migus.dto.HealthResponseDTO;
import br.com.senac.migus.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponseDTO> check() {
        return ResponseEntity.ok(healthService.check());
    }
}
