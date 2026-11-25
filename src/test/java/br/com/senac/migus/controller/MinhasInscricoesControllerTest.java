package br.com.senac.migus.controller;

import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.InscricaoService;
import br.com.senac.migus.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.web.FilterChainProxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MinhasInscricoesControllerTest {
    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private InscricaoRepository inscricoes;
    private JwtService jwt;

    @BeforeAll
    static void iniciar() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
    }

    @AfterAll
    static void fechar() { context.close(); }

    @BeforeEach
    void preparar() {
        var usuarios = context.getBean(UsuariosRepository.class);
        var programas = context.getBean(ProgramaRepository.class);
        inscricoes = context.getBean(InscricaoRepository.class);
        reset(usuarios);
        reset(programas);
        reset(inscricoes);
        jwt = context.getBean(JwtService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
        for (String id : List.of("ana", "bia")) {
            var usuario = new Usuarios();
            usuario.setId(id);
            usuario.setAtivo(true);
            usuario.setTipoUsuario("interessado");
            when(usuarios.findById(id)).thenReturn(Optional.of(usuario));
        }
        var data = Date.from(Instant.parse("2026-11-01T12:00:00Z"));
        when(inscricoes.findByVoluntarioId("ana")).thenReturn(List.of(
                new Inscricao("inscricao-ana", "ana", "programa-1", "APROVADA", data, null, null, null)));
        when(inscricoes.findByVoluntarioId("bia")).thenReturn(List.of(
                new Inscricao("inscricao-bia", "bia", "programa-2", "PENDENTE", data, null, null, null)));
        when(programas.findAllById(List.of("programa-1"))).thenReturn(List.of(new Programa(
                "programa-1", "Leitura", "Descrição", List.of("Cultura"), List.of("segunda"),
                "09:00", "12:00", false, "responsavel", data, null)));
    }

    @Test
    void deveMostrarOrigemTransferidaEDestinoAprovadoSomenteDaConta() throws Exception {
        var entrada = Date.from(Instant.parse("2026-11-01T12:00:00Z"));
        var mudanca = Date.from(Instant.parse("2026-11-05T15:00:00Z"));
        var origem = new Inscricao("origem", "ana", "horta", "APROVADA", entrada, entrada, null, null);
        origem.transferir(mudanca);
        var destino = new Inscricao("destino", "ana", "leitura", "APROVADA", mudanca, mudanca, null, mudanca);
        when(inscricoes.findByVoluntarioId("ana")).thenReturn(List.of(origem, destino));
        var programas = context.getBean(ProgramaRepository.class);
        when(programas.findAllById(List.of("horta", "leitura"))).thenReturn(List.of(
                new Programa("horta", "Horta", null, null, null, null, null, false, "responsavel", entrada, null),
                new Programa("leitura", "Leitura", null, null, null, null, null, true, "responsavel", entrada, null)));

        mvc.perform(get("/api/usuarios/me/inscricoes").header("Authorization", token("ana"))
                .param("voluntarioId", "bia"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].inscricaoId").value("origem"))
                .andExpect(jsonPath("$[0].programaId").value("horta"))
                .andExpect(jsonPath("$[0].programaNome").value("Horta"))
                .andExpect(jsonPath("$[0].status").value("TRANSFERIDA"))
                .andExpect(jsonPath("$[0].dataInscricao", org.hamcrest.Matchers.startsWith("2026-11-01T12:00:00")))
                .andExpect(jsonPath("$[1].inscricaoId").value("destino"))
                .andExpect(jsonPath("$[1].programaId").value("leitura"))
                .andExpect(jsonPath("$[1].programaNome").value("Leitura"))
                .andExpect(jsonPath("$[1].status").value("APROVADA"))
                .andExpect(jsonPath("$[1].dataInscricao", org.hamcrest.Matchers.startsWith("2026-11-05T15:00:00")));
        mvc.perform(get("/api/usuarios/me/inscricoes").header("Authorization", token("bia")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inscricaoId").value("inscricao-bia"));
    }

    @Test
    void deveExibirHistoricoProprioComProgramasSituacoesPeriodosEOrdemRecente() throws Exception {
        var inscricaoAntiga = Date.from(Instant.parse("2026-10-20T12:00:00Z"));
        var inicioAntigo = Date.from(Instant.parse("2026-10-22T12:00:00Z"));
        var atualizadoAntigo = Date.from(Instant.parse("2026-10-25T12:00:00Z"));
        var antiga = new Inscricao("historico-antigo", "ana", "programa-1", "APROVADA",
                inscricaoAntiga, inicioAntigo, null, atualizadoAntigo);

        var inscricaoRecente = Date.from(Instant.parse("2026-11-02T12:00:00Z"));
        var inicioRecente = Date.from(Instant.parse("2026-11-03T12:00:00Z"));
        var recente = new Inscricao("historico-recente", "ana", "programa-2", "APROVADA",
                inscricaoRecente, inicioRecente, null, inicioRecente);
        var fimRecente = Date.from(Instant.parse("2026-11-07T12:00:00Z"));
        recente.sair(fimRecente);

        when(inscricoes.findByVoluntarioId("ana")).thenReturn(List.of(antiga, recente));
        var programas = context.getBean(ProgramaRepository.class);
        when(programas.findAllById(List.of("programa-2", "programa-1"))).thenReturn(List.of(
                new Programa("programa-2", "Leitura", null, null, null, null, null,
                        true, "responsavel", inscricaoRecente, null),
                new Programa("programa-1", "Horta", null, null, null, null, null,
                        true, "responsavel", inscricaoAntiga, null)));

        mvc.perform(get("/api/usuarios/me/historico-participacao")
                        .header("Authorization", token("ana"))
                        .param("voluntarioId", "bia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].inscricaoId").value("historico-recente"))
                .andExpect(jsonPath("$[0].programaNome").value("Leitura"))
                .andExpect(jsonPath("$[0].situacao").value("SAIDA"))
                .andExpect(jsonPath("$[0].inicioParticipacao", org.hamcrest.Matchers.startsWith("2026-11-03T12:00:00")))
                .andExpect(jsonPath("$[0].fimParticipacao", org.hamcrest.Matchers.startsWith("2026-11-07T12:00:00")))
                .andExpect(jsonPath("$[1].inscricaoId").value("historico-antigo"))
                .andExpect(jsonPath("$[1].programaNome").value("Horta"))
                .andExpect(jsonPath("$[1].situacao").value("APROVADA"));

        verify(inscricoes).findByVoluntarioId("ana");
        verify(inscricoes, never()).findByVoluntarioId("bia");
    }

    private String token(String usuario) {
        return "Bearer " + jwt.generateToken(usuario, "ficticio@exemplo.invalid", "interessado");
    }

    @Test
    void deveConsultarSomenteUsuarioDoJwtMesmoComIdentidadeEnviadaPeloCliente() throws Exception {
        mvc.perform(get("/api/usuarios/me/inscricoes")
                .header("Authorization", token("ana"))
                .param("usuarioId", "bia").param("voluntarioId", "bia")
                .contentType("application/json").content("{\"voluntarioId\":\"bia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inscricaoId").value("inscricao-ana"))
                .andExpect(jsonPath("$[0].programaId").value("programa-1"))
                .andExpect(jsonPath("$[0].programaNome").value("Leitura"))
                .andExpect(jsonPath("$[0].status").value("APROVADA"))
                .andExpect(jsonPath("$[0].dataInscricao").isString());
        mvc.perform(get("/api/usuarios/me/inscricoes").header("Authorization", token("bia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inscricaoId").value("inscricao-bia"))
                .andExpect(jsonPath("$[0].programaId").value("programa-2"))
                .andExpect(jsonPath("$[0].programaNome").isEmpty());
        verify(inscricoes, never()).findByProgramaId(any());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveRetornar200ComListaVazia() throws Exception {
        when(inscricoes.findByVoluntarioId("ana")).thenReturn(List.of());
        mvc.perform(get("/api/usuarios/me/inscricoes").header("Authorization", token("ana")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void deveRetornarHistoricoVazioQuandoNaoExistemRegistros() throws Exception {
        when(inscricoes.findByVoluntarioId("ana")).thenReturn(List.of());

        mvc.perform(get("/api/usuarios/me/historico-participacao")
                        .header("Authorization", token("ana")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(context.getBean(ProgramaRepository.class), never()).findAllById(any());
    }

    @Test
    void deveExigirPrincipalParaConsultarHistorico() {
        var service = new InscricaoService(inscricoes,
                context.getBean(ProgramaRepository.class),
                context.getBean(UsuariosRepository.class));
        var controller = new MinhasInscricoesController(service);

        var erro = assertThrows(AuthException.class, () -> controller.consultarHistorico(null));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
        verifyNoInteractions(inscricoes);
    }

    @Test
    void deveBloquearSemTokenOuComTokenInvalido() throws Exception {
        mvc.perform(get("/api/usuarios/me/inscricoes")).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/me/inscricoes").header("Authorization", "Bearer invalido"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/me/historico-participacao"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/me/historico-participacao")
                        .header("Authorization", "Bearer invalido"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inscricoes);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({MinhasInscricoesController.class, SecurityConfig.class, InscricaoService.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean InscricaoRepository inscricoes() { return mock(InscricaoRepository.class); }
        @Bean ProgramaRepository programas() { return mock(ProgramaRepository.class); }
        @Bean UsuariosRepository usuarios() { return mock(UsuariosRepository.class); }
        @Bean JwtService jwt() {
            String chave = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chave, 3600000);
        }
    }
}
