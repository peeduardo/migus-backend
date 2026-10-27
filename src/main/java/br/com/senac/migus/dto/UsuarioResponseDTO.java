package br.com.senac.migus.dto;

import br.com.senac.migus.model.Usuarios;

public record UsuarioResponseDTO(
        String mensagem,
        UsuarioDTO usuario
) {
    public static UsuarioResponseDTO comSucesso(Usuarios usuario) {
        return new UsuarioResponseDTO("Usuário atualizado com sucesso!", UsuarioDTO.fromEntity(usuario));
    }
}
