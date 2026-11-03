package br.com.senac.migus.controller;

import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.UsuariosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuariosAutorizacaoTest {
    private UsuariosRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repository = mock(UsuariosRepository.class);
        var usuario = new Usuarios();
        usuario.setId("usuario-1");
        usuario.setEmail("ana@exemplo.com");
        usuario.setTipoUsuario("interessado");
        usuario.setAtivo(true);
        when(repository.findById("usuario-1")).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuarios.class))).thenAnswer(call -> call.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new UsuariosController(new UsuariosService(repository)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void deveImpedirAlteracaoDoCargo() throws Exception {
        mvc.perform(put("/api/usuarios/usuario-1").principal(() -> "usuario-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@exemplo.com","tipoUsuario":"RESPONSAVEL"}
                        """))
                .andExpect(status().isForbidden());
        verify(repository, never()).save(any());
    }

    @Test
    void deveImpedirAlteracaoDoStatus() throws Exception {
        mvc.perform(put("/api/usuarios/usuario-1").principal(() -> "usuario-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@exemplo.com","ativo":false}
                        """))
                .andExpect(status().isForbidden());
        verify(repository, never()).save(any());
    }

    @Test
    void deveImpedirAlteracaoDeOutraConta() throws Exception {
        mvc.perform(put("/api/usuarios/usuario-1").principal(() -> "outro-usuario")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@exemplo.com"}
                        """))
                .andExpect(status().isForbidden());
        verify(repository, never()).save(any());
    }
}
