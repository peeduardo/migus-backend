package br.com.senac.migus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferenciaRequestDTO(
        @NotBlank(message = "O ID do programa destino é obrigatório") String programaDestinoId,
        @Size(max = 500, message = "O recado deve ter no máximo 500 caracteres") String recado
) {
}
