package br.com.senac.migus.controller;

import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.dto.InscritoDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.InscricaoService;
import br.com.senac.migus.service.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InscricaoControllerTest {

    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuariosRepository usuarios;
    private InscricaoRepository inscricoes;
    private ProgramaRepository programas;
    private String token;

    private static final String USUARIO_ID = "507f1f77bcf86cd799439011";
    private static final String PROGRAMA_ID = "507f1f77bcf86cd799439022";
    private static final String INSCRICAO_ID = "507f1f77bcf86cd799439044";

    @BeforeAll
    static void iniciarContexto() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
    }

    @AfterAll
    static void fecharContexto() { context.close(); }

    @BeforeEach
    void preparar() {
        usuarios = context.getBean(UsuariosRepository.class);
        inscricoes = context.getBean(InscricaoRepository.class);
        programas = context.getBean(ProgramaRepository.class);
        reset(usuarios, inscricoes, programas);
        var usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(inscricoes.save(any(Inscricao.class))).thenAnswer(call -> call.getArgument(0));
        token = context.getBean(JwtService.class).generateToken(USUARIO_ID, "teste@exemplo.com", "interessado");
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    // --- POST /inscrever ---

    @Test
    void deveInscreverVoluntarioERetornar201() throws Exception {
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(inscricoes.save(any())).thenReturn(inscricao);
        when(inscricoes.existsByVoluntarioIdAndProgramaId(any(), any())).thenReturn(false);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));

        mvc.perform(post("/api/programas/{pId}/inscricoes", PROGRAMA_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void deveBloquearInscricaoSemToken() throws Exception {
        mvc.perform(post("/api/programas/{pId}/inscricoes", PROGRAMA_ID))
                .andExpect(status().is4xxClientError());
        verify(inscricoes, never()).save(any());
    }

    // --- PATCH /aprovar ---

    @Test
    void deveAprovarInscricao() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));

        var inscricao = new Inscricao(INSCRICAO_ID, "voluntario-id", PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(inscricoes.save(any())).thenAnswer(call -> call.getArgument(0));

        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/aprovar", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    void deveRetornar403AoAprovarSemSerResponsavel() throws Exception {
        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/aprovar", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // --- PATCH /recusar ---

    @Test
    void deveRecusarInscricao() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));

        var inscricao = new Inscricao(INSCRICAO_ID, "voluntario-id", PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(inscricoes.save(any())).thenAnswer(call -> call.getArgument(0));

        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/recusar", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"));
    }

    // --- GET /inscricoes ---

    @Test
    void deveListarInscritosComStatus() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));
        when(programas.existsById(PROGRAMA_ID)).thenReturn(true);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));

        var inscrito = new InscritoDTO(INSCRICAO_ID, "APROVADA", new Date(),
                USUARIO_ID, "Ana", "ana@exemplo.com", "11999999999");
        when(inscricoes.findByProgramaIdAndStatus(eq(PROGRAMA_ID), eq("APROVADA")))
                .thenReturn(List.of(new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "APROVADA",
                        new Date(), new Date(), null, null)));
        var voluntario = new Usuarios();
        voluntario.setId(USUARIO_ID);
        voluntario.setNome("Ana");
        voluntario.setEmail("ana@exemplo.com");
        when(usuarios.findAllById(any())).thenReturn(List.of(voluntario));

        mvc.perform(get("/api/programas/{pId}/inscricoes?status=APROVADA", PROGRAMA_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Ana"));
    }

    @Test
    void deveRetornar404AoListarInscritosDeProgramaInexistente() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));
        when(programas.existsById(PROGRAMA_ID)).thenReturn(false);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.empty());

        mvc.perform(get("/api/programas/{pId}/inscricoes", PROGRAMA_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- POST /transferir ---

    @Test
    void deveTransferirVoluntario() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));

        String DESTINO_ID = "507f1f77bcf86cd799439099";
        var inscricaoOrigem = new Inscricao(INSCRICAO_ID, "voluntario-id", PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        var novaInscricao = new Inscricao("nova-id", "voluntario-id", DESTINO_ID, "APROVADA",
                new Date(), new Date(), null, new Date());
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricaoOrigem));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(programas.findById(DESTINO_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(DESTINO_ID, "outro-responsavel")));
        when(inscricoes.existsByVoluntarioIdAndProgramaId(any(), eq(DESTINO_ID))).thenReturn(false);
        when(inscricoes.save(any())).thenReturn(novaInscricao);

        mvc.perform(post("/api/programas/{pId}/inscricoes/{iId}/transferir", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"programaDestinoId\":\"" + DESTINO_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programaId").value(DESTINO_ID));
    }

    @Test
    void deveRetornar404AoTransferirInscricaoInexistente() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));
        when(inscricoes.findById(any())).thenReturn(Optional.empty());

        mvc.perform(post("/api/programas/{pId}/inscricoes/{iId}/transferir", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"programaDestinoId\":\"507f1f77bcf86cd799439099\"}"))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /saida ---

    @Test
    void deveRegistrarSaidaDeVoluntario() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));

        var inscricao = new Inscricao(INSCRICAO_ID, "voluntario-id", PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(inscricoes.save(any())).thenAnswer(call -> {
            Inscricao i = call.getArgument(0); i.sair(new Date()); return i;
        });

        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/saida", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAIDA"));
    }

    @Test
    void deveBloquearSaidaSemAutorizacao() throws Exception {
        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/saida", PROGRAMA_ID, INSCRICAO_ID))
                .andExpect(status().is4xxClientError());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveRetornar403AoRegistrarSaidaDeOutroResponsavel() throws Exception {
        // usuário sem tipoUsuario = RESPONSAVEL → 403
        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/saida", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar404AoRegistrarSaidaDeVoluntarioNaoVinculado() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.empty());

        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/saida", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar409AoRegistrarSaidaJaRegistrada() throws Exception {
        var responsavel = new Usuarios();
        responsavel.setId(USUARIO_ID);
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(responsavel));

        var inscricao = new Inscricao(INSCRICAO_ID, "voluntario-id", PROGRAMA_ID, "SAIDA",
                new Date(), new Date(), null, null);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                br.com.senac.migus.ControllerTestHelper.programaAtivo(PROGRAMA_ID, USUARIO_ID)));
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));

        mvc.perform(patch("/api/programas/{pId}/inscricoes/{iId}/saida", PROGRAMA_ID, INSCRICAO_ID)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A saída deste voluntário já foi registrada"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, InscricaoController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean InscricaoRepository inscricaoRepository() { return mock(InscricaoRepository.class); }
        @Bean ProgramaRepository programaRepository() { return mock(ProgramaRepository.class); }
        @Bean UsuariosRepository usuariosRepository() { return mock(UsuariosRepository.class); }
        @Bean InscricaoService inscricaoService(InscricaoRepository i, ProgramaRepository p, UsuariosRepository u) {
            return new InscricaoService(i, p, u);
        }
        @Bean JwtService jwtService() {
            String chave = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chave, 3600000);
        }
    }
}
