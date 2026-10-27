package br.com.senac.migus.service;

import br.com.senac.migus.dto.CadastroRequestDTO;
import br.com.senac.migus.dto.LoginRequestDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuariosRepository usuariosRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuariosRepository, passwordEncoder, jwtService);
    }

    @Test
    void deveCadastrarUsuarioComSenhaCriptografada() {
        var request = cadastroRequest();
        when(usuariosRepository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(usuariosRepository.existsByCpf("12345678900")).thenReturn(false);
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();
        when(passwordEncoder.encode("senha-segura")).thenReturn(bcrypt.encode("senha-segura"));
        when(usuariosRepository.save(any(Usuarios.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = authService.cadastrar(request);

        assertEquals("Ana Silva", resposta.nome());
        assertEquals("ana@exemplo.com", resposta.email());
        assertEquals("interessado", resposta.tipoUsuario());
        ArgumentCaptor<Usuarios> usuarioSalvo = ArgumentCaptor.forClass(Usuarios.class);
        verify(usuariosRepository).save(usuarioSalvo.capture());
        assertTrue(usuarioSalvo.getValue().getSenhaHash().startsWith("$2"));
        assertTrue(bcrypt.matches("senha-segura", usuarioSalvo.getValue().getSenhaHash()));
        verify(passwordEncoder).encode("senha-segura");
    }

    @Test
    void naoDeveCadastrarEmailDuplicado() {
        when(usuariosRepository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.cadastrar(cadastroRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(usuariosRepository, never()).save(any(Usuarios.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void naoDeveCadastrarCpfDuplicado() {
        when(usuariosRepository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(usuariosRepository.existsByCpf("12345678900")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.cadastrar(cadastroRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(usuariosRepository, never()).save(any(Usuarios.class));
    }

    @Test
    void deveAutenticarUsuarioAtivoEEmitirToken() {
        Usuarios usuario = usuario("hash-bcrypt", true);
        when(usuariosRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generateToken("usuario-id", "ana@exemplo.com")).thenReturn("jwt-valido");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        var resposta = authService.autenticar(new LoginRequestDTO(" Ana@Exemplo.com ", "senha-segura"));

        assertEquals("jwt-valido", resposta.token());
        assertEquals("Bearer", resposta.tipo());
        assertEquals(3_600_000L, resposta.expiraEm());
        assertEquals("ana@exemplo.com", resposta.usuario().email());
        verify(jwtService).generateToken("usuario-id", "ana@exemplo.com");
    }

    @Test
    void deveRetornarUnauthorizedQuandoUsuarioNaoExiste() {
        when(usuariosRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@exemplo.com", "senha-segura")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void deveRetornarUnauthorizedQuandoSenhaEstaIncorreta() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario("hash-bcrypt", true)));
        when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@exemplo.com", "senha-errada")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Senha inválida", exception.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void deveBloquearUsuarioInativoMesmoComSenhaCorreta() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario("hash-bcrypt", false)));
        when(passwordEncoder.matches("senha-segura", "hash-bcrypt")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@exemplo.com", "senha-segura")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("inativo"));
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void deveBloquearUsuarioQuandoCampoAtivoEstaNulo() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario("hash-bcrypt", null)));
        when(passwordEncoder.matches("senha-segura", "hash-bcrypt")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@exemplo.com", "senha-segura")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(jwtService, never()).generateToken(any(), any());
    }

    private CadastroRequestDTO cadastroRequest() {
        return new CadastroRequestDTO(" Ana Silva ", " Ana@Exemplo.com ", "senha-segura",
                "12345678900", "11999999999", LocalDate.of(2000, 5, 20));
    }

    private Usuarios usuario(String senhaHash, Boolean ativo) {
        Usuarios usuario = new Usuarios("Ana Silva", "ana@exemplo.com", senhaHash,
                "12345678900", "11999999999", LocalDate.of(2000, 5, 20),
                "interessado", ativo, LocalDate.now(), LocalDate.now());
        ReflectionTestUtils.setField(usuario, "id", "usuario-id");
        return usuario;
    }
}
