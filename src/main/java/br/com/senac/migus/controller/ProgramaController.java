package br.com.senac.migus.controller;

import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.service.ProgramaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/programas")
public class ProgramaController {
    private final ProgramaService programas;

    public ProgramaController(ProgramaService programas) {
        this.programas = programas;
    }

    @PostMapping
    public ResponseEntity<Programa> cadastrar(@Valid @RequestBody ProgramaRequestDTO pedido,
                                             Principal principal) {
        var programa = programas.cadastrar(pedido, principal == null ? null : principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(programa);
    }
}
