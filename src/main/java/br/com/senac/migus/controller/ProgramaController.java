package br.com.senac.migus.controller;

import br.com.senac.migus.dto.DisponibilidadeRequestDTO;
import br.com.senac.migus.dto.ProgramaDetalheDTO;
import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.service.ProgramaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<ProgramaDetalheDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(programas.buscarPorId(id));
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<Programa>> listarAtivos() {
        return ResponseEntity.ok(programas.listarAtivos());
    }

    @PatchMapping("/{id}/disponibilidade")
    public ResponseEntity<Programa> alterarDisponibilidade(
            @PathVariable String id,
            @Valid @RequestBody DisponibilidadeRequestDTO request,
            Principal principal) {

        var programa = programas.alterarDisponibilidade(
                id,
                request.ativo(),
                principal == null ? null : principal.getName());

        return ResponseEntity.ok(programa);
    }

    @GetMapping("/meus")
    public ResponseEntity<List<Programa>> listarMeusProgramas(Principal principal) {
        var resultado = programas.listarMeusProgramas(
                principal == null ? null : principal.getName());

        return ResponseEntity.ok(resultado);
    }
}
