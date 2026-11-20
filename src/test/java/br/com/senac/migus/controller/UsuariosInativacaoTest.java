package br.com.senac.migus.controller;

import br.com.senac.migus.config.CorsConfig;
import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.JwtService;
import br.com.senac.migus.service.UsuariosService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuariosInativacaoTest {
    private static final String RESPONSAVEL_ID = "507f1f77bcf86cd799439011";
    private static final String VOLUNTARIO_ID = "507f1f77bcf86cd799439033";
    private static final String INATIVAR = "/api/usuarios/" + VOLUNTARIO_ID + "/inativar";
    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuariosRepository usuarios;
    private Usuarios responsavel;
    private Usuarios voluntario;
    private String tokenResponsavel;
    private String tokenVoluntario;

    @BeforeAll
    static void iniciarContextoWeb() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
    }

    @AfterAll
    static void fecharContexto() {
        context.close();
    }

    @BeforeEach
    void preparar() {
        usuarios = context.getBean(UsuariosRepository.class);
        reset(usuarios);
        responsavel = new Usuarios();
        responsavel.setId(RESPONSAVEL_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        voluntario = new Usuarios("Bruno Lima", "bruno@exemplo.invalid", "hash", "12345678901", "11999990000",
                LocalDate.of(2000, 1, 10), "VOLUNTARIO", true, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));
        voluntario.setId(VOLUNTARIO_ID);
        when(usuarios.findById(RESPONSAVEL_ID)).thenReturn(Optional.of(responsavel));
        when(usuarios.findById(VOLUNTARIO_ID)).thenReturn(Optional.of(voluntario));
        when(usuarios.desativarParticipante(eq(VOLUNTARIO_ID), any(), any())).thenAnswer(chamada -> {
            voluntario.setAtivo(false);
            return 1L;
        });
        var jwt = context.getBean(JwtService.class);
        tokenResponsavel = jwt.generateToken(RESPONSAVEL_ID, "resp@migus.invalid", "RESPONSAVEL");
        tokenVoluntario = jwt.generateToken(VOLUNTARIO_ID, "bruno@exemplo.invalid", "VOLUNTARIO");
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void responsavelInativaCadastroValidoEPreservaOsDados() throws Exception {
        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Cadastro inativado com sucesso!"))
                .andExpect(jsonPath("$.usuario.id").value(VOLUNTARIO_ID))
                .andExpect(jsonPath("$.usuario.ativo").value(false))
                .andExpect(jsonPath("$.usuario.nome").value("Bruno Lima"))
                .andExpect(jsonPath("$.usuario.email").value("bruno@exemplo.invalid"))
                .andExpect(jsonPath("$.usuario.tipoUsuario").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.usuario.dataCadastro").isNotEmpty())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
        verify(usuarios).desativarParticipante(eq(VOLUNTARIO_ID), eq("VOLUNTARIO"), any());
        verify(usuarios, never()).save(any());
        verify(usuarios, never()).deleteById(any());
    }

    @Test
    void cadastroInativadoNaoPodeMaisUsarASessao() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenVoluntario))
                .andExpect(status().isOk());

        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isOk());

        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenVoluntario))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroJaInativoRetornaConflito() throws Exception {
        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isOk());

        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("O cadastro já está inativo"));
        verify(usuarios).desativarParticipante(eq(VOLUNTARIO_ID), any(), any());
    }

    @Test
    void cadastroInexistenteRetorna404() throws Exception {
        when(usuarios.findById("inexistente")).thenReturn(Optional.empty());

        mvc.perform(patch("/api/usuarios/inexistente/inativar").header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Voluntário não encontrado com o ID: inexistente"));
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"interessado", "VOLUNTARIO"})
    void usuarioSemAutorizacaoNaoInativa(String perfil) throws Exception {
        responsavel.setTipoUsuario(perfil);

        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Somente um responsável ativo pode inativar cadastros"));
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void voluntarioNaoInativaOProprioCadastroPorEstaRota() throws Exception {
        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenVoluntario))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void responsavelNaoInativaOutroResponsavel() throws Exception {
        voluntario.setTipoUsuario("RESPONSAVEL");

        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer " + tokenResponsavel))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Somente cadastros de voluntários podem ser inativados"));
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void semTokenOuComTokenInvalidoNaoInativa() throws Exception {
        mvc.perform(patch(INATIVAR)).andExpect(status().isForbidden());
        mvc.perform(patch(INATIVAR).header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({CorsConfig.class, SecurityConfig.class, UsuariosController.class, UsuariosService.class,
            GlobalExceptionHandler.class})
    static class Config {
        @Bean
        UsuariosRepository usuariosRepository() {
            return mock(UsuariosRepository.class);
        }

        @Bean
        InscricaoRepository inscricaoRepository() {
            return mock(InscricaoRepository.class);
        }

        @Bean
        ProgramaRepository programaRepository() {
            return mock(ProgramaRepository.class);
        }

        @Bean
        JwtService jwtService() {
            String chaveFicticia = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chaveFicticia, 3600000);
        }
    }
}
