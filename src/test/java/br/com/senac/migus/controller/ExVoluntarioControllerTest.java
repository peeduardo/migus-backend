package br.com.senac.migus.controller;

import br.com.senac.migus.config.CorsConfig;
import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.ExVoluntarioService;
import br.com.senac.migus.service.InscricaoService;
import br.com.senac.migus.service.JwtService;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExVoluntarioControllerTest {
    private static final String RESPONSAVEL_ID = "507f1f77bcf86cd799439011";
    private static final String ROTA = "/api/usuarios/ex-voluntarios";
    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuariosRepository usuarios;
    private InscricaoRepository inscricoes;
    private Usuarios responsavel;
    private String token;

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
        inscricoes = context.getBean(InscricaoRepository.class);
        reset(usuarios, inscricoes);
        responsavel = new Usuarios();
        responsavel.setId(RESPONSAVEL_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(RESPONSAVEL_ID)).thenReturn(Optional.of(responsavel));
        when(usuarios.findByAtivoFalse()).thenReturn(List.of());
        when(inscricoes.findByStatus(InscricaoService.STATUS_SAIDA)).thenReturn(List.of());
        token = context.getBean(JwtService.class).generateToken(RESPONSAVEL_ID, "resp@migus.invalid", "RESPONSAVEL");
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    private Usuarios voluntario(String id, String nome, boolean ativo) {
        var usuario = new Usuarios(nome, id + "@exemplo.invalid", "hash-secreto", "12345678901", "11999990000",
                LocalDate.of(2000, 1, 1), "VOLUNTARIO", ativo, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        usuario.setId(id);
        return usuario;
    }

    @Test
    void responsavelConsultaExVoluntariosSemDadosSensiveis() throws Exception {
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(voluntario("u-ana", "Ana", false)));
        var saida = new Inscricao("i-1", "u-bruno", "p-1", "APROVADA", new Date(0), new Date(0), null, null);
        saida.sair(new Date(1_700_000_000_000L));
        when(inscricoes.findByStatus(InscricaoService.STATUS_SAIDA)).thenReturn(List.of(saida));
        when(usuarios.findAllById(any())).thenReturn(List.of(voluntario("u-bruno", "Bruno", true)));

        mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("u-ana"))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].email").value("u-ana@exemplo.invalid"))
                .andExpect(jsonPath("$[0].telefone").value("11999990000"))
                .andExpect(jsonPath("$[0].cadastroAtivo").value(false))
                .andExpect(jsonPath("$[0].saidaRegistrada").value(false))
                .andExpect(jsonPath("$[0].dataCadastro").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value("u-bruno"))
                .andExpect(jsonPath("$[1].cadastroAtivo").value(true))
                .andExpect(jsonPath("$[1].saidaRegistrada").value(true))
                .andExpect(jsonPath("$[1].ultimaSaida").isNotEmpty())
                .andExpect(jsonPath("$[0].cpf").doesNotExist())
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }

    @Test
    void aplicaOsFiltrosDaConsulta() throws Exception {
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(
                voluntario("u-ana", "Ana", false), voluntario("u-joao", "João", false)));

        mvc.perform(get(ROTA).param("nome", "joao").param("situacao", "INATIVO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("u-joao"));
    }

    @Test
    void retornaListaVaziaQuandoNaoHaExVoluntarios() throws Exception {
        mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejeitaSituacaoInvalida() throws Exception {
        mvc.perform(get(ROTA).param("situacao", "TODOS").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Situação inválida. Use INATIVO ou SAIDA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"interessado", "VOLUNTARIO"})
    void impedeConsultaPorUsuarioSemAutorizacao(String perfil) throws Exception {
        responsavel.setTipoUsuario(perfil);

        mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Somente um responsável ativo pode consultar ex-voluntários"));
        verify(inscricoes, never()).findByStatus(any());
    }

    @Test
    void impedeConsultaSemTokenOuComTokenInvalido() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isForbidden());
        mvc.perform(get(ROTA).header("Authorization", "Bearer token-invalido")).andExpect(status().isForbidden());
        verify(usuarios, never()).findByAtivoFalse();
    }

    @Test
    void responsavelInativadoNaoConsulta() throws Exception {
        responsavel.setAtivo(false);

        mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verify(usuarios, never()).findByAtivoFalse();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({CorsConfig.class, SecurityConfig.class, ExVoluntarioController.class, ExVoluntarioService.class,
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
        JwtService jwtService() {
            String chaveFicticia = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chaveFicticia, 3600000);
        }
    }
}
