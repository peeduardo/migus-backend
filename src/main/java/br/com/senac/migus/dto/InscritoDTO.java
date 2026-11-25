package br.com.senac.migus.dto;

import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;

import java.util.Date;

public record InscritoDTO(
        String inscricaoId,
        String status,
        Date dataInscricao,
        String usuarioId,
        String nome,
        String email,
        String telefone,
        Boolean usuarioAtivo,
        String mensagem,
        String recado
) {
    public static InscritoDTO of(Inscricao inscricao, Usuarios usuario) {
        return new InscritoDTO(
                inscricao.getId(),
                inscricao.getStatus(),
                inscricao.getDataInscricao(),
                inscricao.getVoluntarioId(),
                usuario == null ? null : usuario.getNome(),
                usuario == null ? null : usuario.getEmail(),
                usuario == null ? null : usuario.getTelefone(),
                usuario == null ? null : usuario.getAtivo(),
                inscricao.getMensagem(),
                inscricao.getRecado()
        );
    }
}
