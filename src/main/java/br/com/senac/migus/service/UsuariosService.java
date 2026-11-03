package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.dto.UsuarioDTO;

import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Service
public class UsuariosService {

    private final UsuariosRepository usuariosRepository;

    public UsuariosService(UsuariosRepository usuariosRepository) {
        this.usuariosRepository = usuariosRepository;
    }

    public UsuarioDTO getMeuPerfil(String userId) {
        Usuarios usuario = usuariosRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return UsuarioDTO.fromEntity(usuario);
    }

    public UsuarioResponseDTO atualizar(String id, UsuarioUpdateRequestDTO dto, String usuarioAutenticadoId) {
        if (usuarioAutenticadoId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        if (!usuarioAutenticadoId.equals(id)) {
            throw new AuthException("Você só pode atualizar sua própria conta", HttpStatus.FORBIDDEN);
        }
        if (dto.tipoUsuario() != null || dto.ativo() != null) {
            throw new AuthException("Não é permitido alterar perfil ou status nesta operação", HttpStatus.FORBIDDEN);
        }
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        if (dto.email() != null && !dto.email().equalsIgnoreCase(usuario.getEmail())) {
            usuariosRepository.findByEmail(dto.email()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new IllegalArgumentException("O e-mail informado já está em uso por outro usuário");
                }
            });
        }

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        if (dto.telefone() != null) {
            usuario.setTelefone(dto.telefone());
        }
        if (dto.dataNascimento() != null) {
            usuario.setDataNascimento(dto.dataNascimento());
        }
        usuario.setDataAtualizacao(LocalDate.now());

        Usuarios salvo = usuariosRepository.save(usuario);
        return UsuarioResponseDTO.comSucesso(salvo);
    }
}
