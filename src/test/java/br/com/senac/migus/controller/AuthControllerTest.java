package br.com.senac.migus.controller;

import br.com.senac.migus.config.SecurityConfig;
import br.com.senac.migus.dto.*;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.GlobalExceptionHandler;
import br.com.senac.migus.repository.UsuariosRepository;
import br.com.senac.migus.service.AuthService;
import br.com.senac.migus.service.JwtService;
import br.com.senac.migus.model.Usuarios;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private static AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private AuthService authService;

    private static final String CADASTRO = """
            {"nome":"Ana","email":"ana@exemplo.com","senha":"senha123",
             "cpf":"12345678900","telefone":"11999999999","dataNascimento":"2000-05-20"}
            """;

    private static final String LOGIN = """
            {"email":"ana@exemplo.com","senha":"senha123"}
            """;

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
        authService = context.getBean(AuthService.class);
        reset(authService);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void deveCadastrarUsuarioERetornar201() throws Exception {
        var usuarioDTO = new UsuarioDTO("id-1", "Ana", "ana@exemplo.com", "12345678900",
                "11999999999", LocalDate.of(2000, 5, 20), "interessado", true,
                LocalDate.now(), LocalDate.now());
        when(authService.cadastrar(any())).thenReturn(new UsuarioResponseDTO("Cadastro realizado com sucesso!", usuarioDTO));

        mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Cadastro realizado com sucesso!"))
                .andExpect(jsonPath("$.usuario.email").value("ana@exemplo.com"));
    }

    @Test
    void deveRetornar409QuandoEmailJaExiste() throws Exception {
        when(authService.cadastrar(any())).thenThrow(new AuthException("Já existe um usuário cadastrado com este e-mail", HttpStatus.CONFLICT));

        mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com este e-mail"));
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosFaltam() throws Exception {
        mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(authService, never()).cadastrar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567890A", "1234567890", "123456789012", "123.456.789-00"})
    void deveRejeitarCpfForaDoFormato(String cpf) throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(CADASTRO.replace("12345678900", cpf)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    private MockMvc cadastroComRepository(UsuariosRepository usuarios) {
        var service = new AuthService(usuarios, mock(PasswordEncoder.class), mock(JwtService.class));
        return MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void deveAceitarOnzeDigitosIncluindoZeroInicial() throws Exception {
        var usuarios = mock(UsuariosRepository.class);
        when(usuarios.save(any(Usuarios.class))).thenAnswer(call -> call.getArgument(0));
        cadastroComRepository(usuarios).perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(CADASTRO.replace("12345678900", "01234567890")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.cpf").value("01234567890"));
    }

    @Test
    void deveRejeitarCpfJaCadastrado() throws Exception {
        var usuarios = mock(UsuariosRepository.class);
        when(usuarios.existsByCpf("12345678900")).thenReturn(true);
        cadastroComRepository(usuarios).perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(CADASTRO))
                .andExpect(status().isConflict());
        verify(usuarios, never()).save(any());
    }

    @Test
    void deveAutenticarERetornarToken() throws Exception {
        var usuarioDTO = new UsuarioDTO("id-1", "Ana", "ana@exemplo.com", "12345678900",
                "11999999999", LocalDate.of(2000, 5, 20), "interessado", true,
                LocalDate.now(), LocalDate.now());
        when(authService.autenticar(any())).thenReturn(
                new AuthResponseDTO("jwt-token", "Bearer", 3600000L, usuarioDTO));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        when(authService.autenticar(any())).thenThrow(new AuthException("Senha inválida", HttpStatus.UNAUTHORIZED));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Senha inválida"));
    }

    @Test
    void deveRetornar400QuandoLoginSemCampos() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(authService, never()).autenticar(any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean UsuariosRepository usuariosRepository() { return mock(UsuariosRepository.class); }
        @Bean JwtService jwtService() {
            String chave = Base64.getEncoder().encodeToString(
                    "chave-ficticia-apenas-para-testes-123456789".getBytes(StandardCharsets.UTF_8));
            return new JwtService(chave, 3600000);
        }
    }
}
