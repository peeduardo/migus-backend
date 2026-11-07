package br.com.senac.migus.dto;

import jakarta.validation.constraints.NotBlank;

public record TransferenciaRequestDTO(
        @NotBlank(message = "O ID do programa destino é obrigatório") String programaDestinoId
) {
}
