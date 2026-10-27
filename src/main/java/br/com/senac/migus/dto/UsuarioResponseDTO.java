package br.com.senac.migus.dto;

import br.com.senac.migus.model.Usuarios;

public record UsuarioResponseDTO(String id, String nome, String email, String tipoUsuario) {
    public static UsuarioResponseDTO from(Usuarios usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipoUsuario());
    }
}
