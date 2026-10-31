package br.com.senac.migus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProgramaRequestDTO(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        @NotEmpty(message = "Categoria é obrigatória") List<@NotBlank String> categoria,
        @NotEmpty(message = "Dias disponíveis são obrigatórios") List<@NotBlank String> diasDisponiveis,
        @NotBlank(message = "Horário de início é obrigatório") String horarioInicio,
        @NotBlank(message = "Horário de fim é obrigatório") String horarioFim,
        @NotNull(message = "Ativo é obrigatório") Boolean ativo
) {
}
