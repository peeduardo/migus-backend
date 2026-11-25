package br.com.senac.migus.dto;

import jakarta.validation.constraints.Size;

public record InscricaoRequestDTO(
        @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres") String mensagem
) {
}
