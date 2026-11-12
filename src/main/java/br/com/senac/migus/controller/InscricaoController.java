package br.com.senac.migus.controller;

import br.com.senac.migus.dto.InscritoDTO;
import br.com.senac.migus.dto.TransferenciaRequestDTO;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.service.InscricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/programas/{programaId}/inscricoes")
public class InscricaoController {
    private final InscricaoService inscricoes;

    public InscricaoController(InscricaoService inscricoes) {
        this.inscricoes = inscricoes;
    }

    @PostMapping
    public ResponseEntity<Inscricao> inscrever(@PathVariable String programaId, Principal principal) {
        var inscricao = inscricoes.inscrever(programaId, principal == null ? null : principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricao);
    }

    @PatchMapping("/{inscricaoId}/aprovar")
    public ResponseEntity<Inscricao> aprovar(@PathVariable String programaId, @PathVariable String inscricaoId,
                                            Principal principal) {
        return ResponseEntity.ok(inscricoes.aprovar(programaId, inscricaoId,
                principal == null ? null : principal.getName()));
    }

    @PatchMapping("/{inscricaoId}/recusar")
    public ResponseEntity<Inscricao> recusar(@PathVariable String programaId, @PathVariable String inscricaoId,
                                            Principal principal) {
        return ResponseEntity.ok(inscricoes.recusar(programaId, inscricaoId,
                principal == null ? null : principal.getName()));
    }

    @PatchMapping("/{inscricaoId}/saida")
    public ResponseEntity<Inscricao> registrarSaida(
            @PathVariable String programaId,
            @PathVariable String inscricaoId,
            Principal principal) {
        return ResponseEntity.ok(inscricoes.registrarSaida(programaId, inscricaoId,
                principal == null ? null : principal.getName()));
    }

    @PostMapping("/{inscricaoId}/transferir")
    public ResponseEntity<Inscricao> transferir(
            @PathVariable String programaId,
            @PathVariable String inscricaoId,
            @Valid @RequestBody TransferenciaRequestDTO request,
            Principal principal) {
        var novaInscricao = inscricoes.transferir(inscricaoId, request.programaDestinoId(),
                principal == null ? null : principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(novaInscricao);
    }

    @GetMapping
    public ResponseEntity<List<InscritoDTO>> listarInscritos(
            @PathVariable String programaId,
            @RequestParam(required = false) String status,
            Principal principal) {
        return ResponseEntity.ok(inscricoes.listarInscritos(programaId, status,
                principal == null ? null : principal.getName()));
    }
}
