package br.com.senac.migus.controller;

import br.com.senac.migus.dto.ExVoluntarioDTO;
import br.com.senac.migus.service.ExVoluntarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios/ex-voluntarios")
public class ExVoluntarioController {
    private final ExVoluntarioService exVoluntarios;

    public ExVoluntarioController(ExVoluntarioService exVoluntarios) {
        this.exVoluntarios = exVoluntarios;
    }

    @GetMapping
    public ResponseEntity<List<ExVoluntarioDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String situacao,
            Principal principal) {
        return ResponseEntity.ok(exVoluntarios.listar(nome, situacao,
                principal == null ? null : principal.getName()));
    }
}
