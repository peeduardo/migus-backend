package br.com.senac.migus.service;

import br.com.senac.migus.dto.AuthResponseDTO;
import br.com.senac.migus.dto.CadastroRequestDTO;
import br.com.senac.migus.dto.LoginRequestDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;

@Service
public class AuthService {
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuariosRepository usuariosRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UsuarioResponseDTO cadastrar(CadastroRequestDTO request) {
        String email = normalizarEmail(request.email());
        if (usuariosRepository.existsByEmail(email)) {
            throw new AuthException("Já existe um usuário cadastrado com este e-mail", HttpStatus.CONFLICT);
        }
        if (usuariosRepository.existsByCpf(request.cpf())) {
            throw new AuthException("Já existe um usuário cadastrado com este CPF", HttpStatus.CONFLICT);
        }

        Usuarios usuario = new Usuarios(
                request.nome().trim(), email, passwordEncoder.encode(request.senha()),
                request.cpf(), request.telefone(), request.dataNascimento(),
                "interessado", true, LocalDate.now(), LocalDate.now());
        return UsuarioResponseDTO.from(usuariosRepository.save(usuario));
    }

    public AuthResponseDTO autenticar(LoginRequestDTO request) {
        Usuarios usuario = usuariosRepository.findByEmail(normalizarEmail(request.email()))
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new AuthException("Senha inválida", HttpStatus.UNAUTHORIZED);
        }
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AuthException("Usuário inativo. Entre em contato com o suporte.", HttpStatus.FORBIDDEN);
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail());
        return new AuthResponseDTO(token, "Bearer", jwtService.getExpirationMs(),
                UsuarioResponseDTO.from(usuario));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
