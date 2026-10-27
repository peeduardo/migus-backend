package br.com.senac.migus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastroRequestDTO(
                @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres") String nome,
                @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
                @NotBlank(message = "Senha é obrigatória") @Size(min = 8, max = 72, message = "Senha deve ter no mínimo 8 caracteres") String senha,
                @NotBlank(message = "CPF é obrigatório") String cpf,
                String telefone,
                @Past(message = "Data de nascimento deve ser anterior à data atual") LocalDate dataNascimento) {
}
