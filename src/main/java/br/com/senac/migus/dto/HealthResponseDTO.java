package br.com.senac.migus.dto;

import java.time.LocalDateTime;

public record HealthResponseDTO(String status, LocalDateTime timestamp) {
}
