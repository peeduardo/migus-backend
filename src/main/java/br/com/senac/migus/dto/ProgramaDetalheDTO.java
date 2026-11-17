package br.com.senac.migus.dto;

import br.com.senac.migus.model.Programa;

import java.util.Date;
import java.util.List;

public record ProgramaDetalheDTO(
        String id,
        String nome,
        String descricao,
        List<String> categoria,
        List<String> diasDisponiveis,
        String horarioInicio,
        String horarioFim,
        Boolean ativo,
        Date dataCadastro
) {
    public static ProgramaDetalheDTO fromEntity(Programa programa) {
        return new ProgramaDetalheDTO(
                programa.getId(),
                programa.getNome(),
                programa.getDescricao(),
                programa.getCategoria(),
                programa.getDiasDisponiveis(),
                programa.getHorarioInicio(),
                programa.getHorarioFim(),
                programa.getAtivo(),
                programa.getDataCadastro()
        );
    }
}
