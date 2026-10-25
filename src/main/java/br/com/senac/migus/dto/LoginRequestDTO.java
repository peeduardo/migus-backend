package br.com.senac.migus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
                @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
                @NotBlank(message = "Senha é obrigatória") String senha) {
}
