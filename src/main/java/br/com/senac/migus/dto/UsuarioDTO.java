package br.com.senac.migus.dto;

import br.com.senac.migus.model.Usuarios;
import java.time.LocalDate;

public record UsuarioDTO(
        String id,
        String nome,
        String email,
        String cpf,
        String telefone,
        LocalDate dataNascimento,
        String tipoUsuario,
        Boolean ativo,
        LocalDate dataCadastro,
        LocalDate dataAtualizacao
) {
    public static UsuarioDTO fromEntity(Usuarios usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getDataNascimento(),
                usuario.getTipoUsuario(),
                usuario.getAtivo(),
                usuario.getDataCadastro(),
                usuario.getDataAtualizacao()
        );
    }
}
