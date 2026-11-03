package br.com.senac.migus.service;

import br.com.senac.migus.dto.UsuarioDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuariosServiceTest {

    @Mock
    private UsuariosRepository usuariosRepository;

    @InjectMocks
    private UsuariosService usuariosService;

    private Usuarios usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioExistente = new Usuarios(
                "João Silva",
                "joao@email.com",
                "hash123",
                "12345678901",
                "11988887777",
                LocalDate.of(1995, 3, 15),
                "VOLUNTARIO",
                true,
                LocalDate.of(2026, 1, 1),
                null
        );
        usuarioExistente.setId("user-123");
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João da Silva Atualizado",
                "joao.novo@email.com",
                "11999998888",
                LocalDate.of(1995, 3, 20),
                null,
                null
        );

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.findByEmail("joao.novo@email.com")).thenReturn(Optional.empty());
        when(usuariosRepository.save(any(Usuarios.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizar("user-123", dto, "user-123");

        assertThat(response).isNotNull();
        assertThat(response.mensagem()).isEqualTo("Usuário atualizado com sucesso!");
        assertThat(response.usuario().id()).isEqualTo("user-123");
        assertThat(response.usuario().nome()).isEqualTo("João da Silva Atualizado");
        assertThat(response.usuario().email()).isEqualTo("joao.novo@email.com");
        assertThat(response.usuario().telefone()).isEqualTo("11999998888");
        assertThat(response.usuario().dataNascimento()).isEqualTo(LocalDate.of(1995, 3, 20));
        assertThat(response.usuario().tipoUsuario()).isEqualTo("VOLUNTARIO");
        assertThat(response.usuario().ativo()).isTrue();
        assertThat(response.usuario().dataAtualizacao()).isEqualTo(LocalDate.now());

        verify(usuariosRepository).save(usuarioExistente);
    }

    @Test
    void devePermitirAtualizarMantendoOMesmoEmail() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João Alterado",
                "joao@email.com",
                "11977776666",
                null,
                null,
                null
        );

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.save(any(Usuarios.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizar("user-123", dto, "user-123");

        assertThat(response.mensagem()).isEqualTo("Usuário atualizado com sucesso!");
        assertThat(response.usuario().nome()).isEqualTo("João Alterado");
        assertThat(response.usuario().email()).isEqualTo("joao@email.com");
        assertThat(response.usuario().telefone()).isEqualTo("11977776666");

        verify(usuariosRepository, never()).findByEmail(any());
        verify(usuariosRepository).save(usuarioExistente);
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "Inexistente",
                "inexistente@email.com",
                "11900000000",
                null,
                null,
                null
        );

        when(usuariosRepository.findById("id-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.atualizar("id-invalido", dto, "id-invalido"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com o ID: id-invalido");

        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoEmailPertenceAOutroUsuario() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João Silva",
                "outro@email.com",
                "11988887777",
                null,
                null,
                null
        );

        Usuarios outroUsuario = new Usuarios();
        outroUsuario.setId("user-456");
        outroUsuario.setEmail("outro@email.com");

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.findByEmail("outro@email.com")).thenReturn(Optional.of(outroUsuario));

        assertThatThrownBy(() -> usuariosService.atualizar("user-123", dto, "user-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O e-mail informado já está em uso por outro usuário");

        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void getMeuPerfil_deveRetornarDadosDoUsuario() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));

        UsuarioDTO dto = usuariosService.getMeuPerfil("user-123");

        assertThat(dto.id()).isEqualTo("user-123");
        assertThat(dto.nome()).isEqualTo("João Silva");
        assertThat(dto.email()).isEqualTo("joao@email.com");
    }

    @Test
    void getMeuPerfil_deveLancarExcecaoSeNaoEncontrado() {
        when(usuariosRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.getMeuPerfil("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void getMeuPerfil_naoDeveExporSenhaHash() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));

        UsuarioDTO dto = usuariosService.getMeuPerfil("user-123");

        boolean temCampoSenha = java.util.Arrays.stream(dto.getClass().getRecordComponents())
                .anyMatch(c -> c.getName().toLowerCase().contains("senha"));

        assertThat(temCampoSenha).isFalse();
    }
}
