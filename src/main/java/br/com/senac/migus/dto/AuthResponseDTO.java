package br.com.senac.migus.dto;

public record AuthResponseDTO(String token, String tipo, long expiraEm, UsuarioDTO usuario) {
}
