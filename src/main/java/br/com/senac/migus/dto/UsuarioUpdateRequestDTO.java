package br.com.senac.migus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UsuarioUpdateRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        String telefone,
        LocalDate dataNascimento,
        String tipoUsuario,
        Boolean ativo
) {
}
