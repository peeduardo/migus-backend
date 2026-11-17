package br.com.senac.migus.controller;

import br.com.senac.migus.config.CorsConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;
import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgramaControllerTest {
    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuariosRepository usuarios;
    private ProgramaRepository programas;
    private String token;
    private static final String USUARIO_ID = "507f1f77bcf86cd799439011";
    private static final String PEDIDO = """
            {"nome":"Apoio","descricao":"Apoio à comunidade","categoria":["Educação"],
             "diasDisponiveis":["segunda"],"horarioInicio":"09:00","horarioFim":"12:00","ativo":true}
            """;

    @BeforeAll
    static void iniciarContextoWeb() {
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
        programas = context.getBean(ProgramaRepository.class);
        reset(usuarios);
        reset(programas);
        var usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setTipoUsuario("RESPONSAVEL");
        usuario.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(programas.save(any(Programa.class))).thenAnswer(call -> call.getArgument(0));
        token = context.getBean(JwtService.class).generateToken(USUARIO_ID, "teste@exemplo.com", "interessado");
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void deveCadastrarComTokenDoResponsavel() throws Exception {
        mvc.perform(post("/api/programas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Apoio"))
                .andExpect(jsonPath("$.responsavelId").value(USUARIO_ID))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.dataCadastro").isNotEmpty());
        verify(programas).save(any(Programa.class));
    }

    @Test
    void deveListarProgramasAtivosComToken() throws Exception {
        var programa = new Programa("507f1f77bcf86cd799439022", "Apoio", "Apoio à comunidade",
                java.util.List.of("Educação"), java.util.List.of("segunda"), "09:00", "12:00", true,
                USUARIO_ID, new java.util.Date(), null);
        when(programas.findByAtivoTrue()).thenReturn(java.util.List.of(programa));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/programas/ativos")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("507f1f77bcf86cd799439022"))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveBloquearListagemDeAtivosSemToken() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/programas/ativos"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(programas);
    }

    @Test
    void deveBloquearSemToken() throws Exception {
        mvc.perform(post("/api/programas").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarios, programas);
    }

    @Test
    void deveBloquearTokenInvalido() throws Exception {
        mvc.perform(post("/api/programas").header("Authorization", "Bearer token-invalido")
                .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarios, programas);
    }

    @Test
    void deveBloquearUsuarioQueNaoExisteMais() throws Exception {
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.empty());
        mvc.perform(post("/api/programas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isUnauthorized());
        verify(programas, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "descricao", "categoria", "diasDisponiveis", "horarioInicio", "horarioFim", "ativo"})
    void deveRejeitarCampoAusenteOuNulo(String campo) throws Exception {
        var json = (ObjectNode) new JsonMapper().readTree(PEDIDO);
        json.remove(campo);
        conferirPedidoInvalido(json.toString());
        json.putNull(campo);
        conferirPedidoInvalido(json.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "descricao", "horarioInicio", "horarioFim"})
    void deveRejeitarTextoVazioOuEmBranco(String campo) throws Exception {
        var json = (ObjectNode) new JsonMapper().readTree(PEDIDO);
        json.put(campo, "");
        conferirPedidoInvalido(json.toString());
        json.put(campo, "   ");
        conferirPedidoInvalido(json.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria", "diasDisponiveis"})
    void deveRejeitarListaVaziaOuComItemInvalido(String campo) throws Exception {
        var json = (ObjectNode) new JsonMapper().readTree(PEDIDO);
        json.putArray(campo);
        conferirPedidoInvalido(json.toString());
        json.putArray(campo).add("");
        conferirPedidoInvalido(json.toString());
        json.putArray(campo).add("   ");
        conferirPedidoInvalido(json.toString());
        json.putArray(campo).addNull();
        conferirPedidoInvalido(json.toString());
    }

    @Test
    void deveRejeitarJsonOuTipoDeDadoInvalido() throws Exception {
        conferirPedidoInvalido("{");
        conferirPedidoInvalido(PEDIDO.replace("\"ativo\":true", "\"ativo\":\"invalido\""));
    }

    @Test
    void naoDeveUsarResponsavelOuAuditoriaEnviadosPeloCliente() throws Exception {
        var json = (ObjectNode) new JsonMapper().readTree(PEDIDO);
        json.put("responsavelId", "507f1f77bcf86cd799439099");
        json.put("id", "507f1f77bcf86cd799439098");
        json.put("dataCadastro", "2000-01-01");
        json.put("dataAtualizacao", "2000-01-02");
        mvc.perform(post("/api/programas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavelId").value(USUARIO_ID))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not("507f1f77bcf86cd799439098")))
                .andExpect(jsonPath("$.dataCadastro").value(org.hamcrest.Matchers.not("2000-01-01")))
                .andExpect(jsonPath("$.dataAtualizacao").doesNotExist());
    }

    private void conferirPedidoInvalido(String json) throws Exception {
        mvc.perform(post("/api/programas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verify(programas, never()).save(any());
    }

    @Test
    void deveBuscarProgramaPorId() throws Exception {
        var programa = new Programa("507f1f77bcf86cd799439022", "Apoio", "Apoio à comunidade",
                java.util.List.of("Educação"), java.util.List.of("segunda"), "09:00", "12:00", true,
                USUARIO_ID, new java.util.Date(), null);
        when(programas.findById("507f1f77bcf86cd799439022")).thenReturn(Optional.of(programa));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/programas/507f1f77bcf86cd799439022")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("507f1f77bcf86cd799439022"))
                .andExpect(jsonPath("$.nome").value("Apoio"))
                .andExpect(jsonPath("$.responsavelId").doesNotExist());
    }

    @Test
    void deveBuscarProgramaPorIdInexistente() throws Exception {
        when(programas.findById("id-invalido")).thenReturn(Optional.empty());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/programas/id-invalido")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Programa não encontrado com o ID: id-invalido"));
    }

    @ParameterizedTest
    @CsvSource({"interessado,true", "VOLUNTARIO,true"})
    void deveRetornar403ParaPerfilSemPermissao(String perfil, boolean ativo) throws Exception {
        var usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setTipoUsuario(perfil);
        usuario.setAtivo(ativo);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        mvc.perform(post("/api/programas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Somente um responsável ativo pode cadastrar programas"));
        verify(programas, never()).save(any());
    }

    @Test
    void deveProtegerAtualizacaoComJwtReal() throws Exception {
        String dados = "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/usuarios/" + USUARIO_ID)
                .contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/usuarios/outra-conta")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/usuarios/" + USUARIO_ID)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(dados.replace("}", ",\"tipoUsuario\":\"RESPONSAVEL\",\"ativo\":true}")))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).save(any());
    }

    private static final String DESATIVAR = "/api/usuarios/me/desativar";

    @ParameterizedTest
    @ValueSource(strings = {"interessado", "VOLUNTARIO"})
    void desativaParticipanteERecusaMesmoJwtNasProximasOperacoes(String perfil) throws Exception {
        var participante = usuarios.findById(USUARIO_ID).orElseThrow();
        participante.setTipoUsuario(perfil);
        when(usuarios.desativarParticipante(eq(USUARIO_ID), eq(perfil), any())).thenAnswer(chamada -> {
            participante.setAtivo(false);
            return 1L;
        });
        String autorizacao = "Bearer " + token;
        mvc.perform(patch(DESATIVAR).header("Authorization", autorizacao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/me").header("Authorization", autorizacao)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/programas").header("Authorization", autorizacao)).andExpect(status().isUnauthorized());
        mvc.perform(patch(DESATIVAR).header("Authorization", autorizacao)).andExpect(status().isUnauthorized());
        verify(usuarios).desativarParticipante(eq(USUARIO_ID), eq(perfil), any());
        verifyNoInteractions(programas);

        var outraConta = new Usuarios();
        outraConta.setId("outra");
        outraConta.setAtivo(true);
        when(usuarios.findById("outra")).thenReturn(Optional.of(outraConta));
        String outroJwt = context.getBean(JwtService.class).generateToken("outra", "outra@exemplo.invalid", perfil);
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + outroJwt))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("outra"));
    }

    @Test
    void responsavelNoBancoNaoPodeDesativarMesmoComJwtDeParticipante() throws Exception {
        mvc.perform(patch(DESATIVAR).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void rejeitaIdentidadeEnviadaPeloCliente() throws Exception {
        usuarios.findById(USUARIO_ID).orElseThrow().setTipoUsuario("VOLUNTARIO");
        mvc.perform(patch(DESATIVAR + "?id=outra").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(DESATIVAR).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"outra\"}"))
                .andExpect(status().isBadRequest());
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void rejeitaDesativacaoSemIdentidadeValida() throws Exception {
        String chave = Base64.getEncoder().encodeToString(
                "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
        String vencido = new JwtService(chave, -60000).generateToken(USUARIO_ID, "ana@exemplo.invalid", "VOLUNTARIO");
        mvc.perform(patch(DESATIVAR)).andExpect(status().isForbidden());
        for (String invalido : new String[]{"invalido", vencido}) {
            mvc.perform(patch(DESATIVAR).header("Authorization", "Bearer " + invalido)).andExpect(status().isForbidden());
        }
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.empty());
        mvc.perform(patch(DESATIVAR).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        verify(usuarios, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void permitePreflightDaDesativacaoSemAutenticacao() throws Exception {
        mvc.perform(options(DESATIVAR).header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({CorsConfig.class, SecurityConfig.class, ProgramaController.class, ProgramaService.class,
            UsuariosController.class, UsuariosService.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean UsuariosRepository usuariosRepository() { return mock(UsuariosRepository.class); }
        @Bean ProgramaRepository programaRepository() { return mock(ProgramaRepository.class); }
        @Bean JwtService jwtService() {
            String chaveFicticia = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chaveFicticia, 3600000);
        }
    }
}
