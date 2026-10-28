package br.com.senac.migus.controller;

import br.com.senac.migus.dto.UsuarioDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.service.UsuariosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuariosControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuariosService usuariosService;

    @InjectMocks
    private UsuariosController usuariosController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(usuariosController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar200EMensagemDeSucessoAoAtualizarUsuario() throws Exception {
        String requestJson = """
                {
                    "nome": "Maria Souza",
                    "email": "maria@email.com",
                    "telefone": "11988887777",
                    "dataNascimento": "1990-05-20",
                    "tipoUsuario": "VOLUNTARIO",
                    "ativo": true
                }
                """;

        UsuarioDTO usuarioDTO = new UsuarioDTO(
                "user-1",
                "Maria Souza",
                "maria@email.com",
                "12345678901",
                "11988887777",
                LocalDate.of(1990, 5, 20),
                "VOLUNTARIO",
                true,
                LocalDate.of(2026, 1, 1),
                LocalDate.now()
        );

        UsuarioResponseDTO responseDTO = new UsuarioResponseDTO(
                "Usuário atualizado com sucesso!",
                usuarioDTO
        );

        when(usuariosService.atualizar(eq("user-1"), any(UsuarioUpdateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/usuarios/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Usuário atualizado com sucesso!"))
                .andExpect(jsonPath("$.usuario.id").value("user-1"))
                .andExpect(jsonPath("$.usuario.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.usuario.email").value("maria@email.com"))
                .andExpect(jsonPath("$.usuario.telefone").value("11988887777"))
                .andExpect(jsonPath("$.usuario.tipoUsuario").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.usuario.ativo").value(true));
    }

    @Test
    void deveRetornar404QuandoUsuarioNaoEncontrado() throws Exception {
        String requestJson = """
                {
                    "nome": "Maria Souza",
                    "email": "maria@email.com",
                    "telefone": "11988887777"
                }
                """;

        when(usuariosService.atualizar(eq("id-inexistente"), any(UsuarioUpdateRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado com o ID: id-inexistente"));

        mockMvc.perform(put("/api/usuarios/id-inexistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado com o ID: id-inexistente"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deveRetornar400QuandoNomeEstiverEmBranco() throws Exception {
        String requestJson = """
                {
                    "nome": "",
                    "email": "maria@email.com",
                    "telefone": "11988887777"
                }
                """;

        mockMvc.perform(put("/api/usuarios/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Nome é obrigatório")));
    }

    @Test
    void deveRetornar400QuandoEmailForInvalido() throws Exception {
        String requestJson = """
                {
                    "nome": "Maria Souza",
                    "email": "email-invalido",
                    "telefone": "11988887777"
                }
                """;

        mockMvc.perform(put("/api/usuarios/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("E-mail inválido")));
    }

    @Test
    void deveRetornar400QuandoEmailJaEstiverEmUso() throws Exception {
        String requestJson = """
                {
                    "nome": "Maria Souza",
                    "email": "email.duplicado@email.com",
                    "telefone": "11988887777"
                }
                """;

        when(usuariosService.atualizar(eq("user-1"), any(UsuarioUpdateRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("O e-mail informado já está em uso por outro usuário"));

        mockMvc.perform(put("/api/usuarios/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("O e-mail informado já está em uso por outro usuário"));
    }
    @Test
    void getMeuPerfil_deveRetornar200ComDadosDoUsuario() throws Exception {
        UsuarioDTO dto = new UsuarioDTO("user-1", "João", "joao@email.com",
                "123.456.789-00", "11999999999", LocalDate.of(1990, 1, 1),
                "voluntario", true, LocalDate.now(), LocalDate.now());

        when(usuariosService.getMeuPerfil("user-1")).thenReturn(dto);

        var auth = new UsernamePasswordAuthenticationToken(
                "user-1", null, AuthorityUtils.NO_AUTHORITIES);

        mockMvc.perform(get("/api/usuarios/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }
}
