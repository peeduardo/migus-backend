package br.com.senac.migus.service;

import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UsuariosService {

    private final UsuariosRepository usuariosRepository;

    public UsuariosService(UsuariosRepository usuariosRepository) {
        this.usuariosRepository = usuariosRepository;
    }

    public UsuarioResponseDTO atualizar(String id, UsuarioUpdateRequestDTO dto) {
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
        if (dto.tipoUsuario() != null) {
            usuario.setTipoUsuario(dto.tipoUsuario());
        }
        if (dto.ativo() != null) {
            usuario.setAtivo(dto.ativo());
        }
        usuario.setDataAtualizacao(LocalDate.now());

        Usuarios salvo = usuariosRepository.save(usuario);
        return UsuarioResponseDTO.comSucesso(salvo);
    }
}
