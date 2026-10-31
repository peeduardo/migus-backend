package br.com.senac.migus.controller;

import br.com.senac.migus.dto.UsuarioDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.service.UsuariosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final UsuariosService usuariosService;

    public UsuariosController(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getMeuPerfil(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UsuarioDTO usuario = usuariosService.getMeuPerfil(userId);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody UsuarioUpdateRequestDTO dto) {
        UsuarioResponseDTO response = usuariosService.atualizar(id, dto);
        return ResponseEntity.ok(response);
    }
}
