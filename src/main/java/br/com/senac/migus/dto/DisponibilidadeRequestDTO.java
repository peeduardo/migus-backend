package br.com.senac.migus.dto;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadeRequestDTO(
        @NotNull(message = "O estado de disponibilidade é obrigatório") 
        Boolean ativo) {
}