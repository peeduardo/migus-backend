package br.com.senac.migus.dto;

import jakarta.validation.constraints.Size;

public record DecisaoRequestDTO(
        @Size(max = 500, message = "O recado deve ter no máximo 500 caracteres") String recado
) {
}
