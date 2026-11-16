package br.com.senac.migus.controller;

import br.com.senac.migus.dto.HistoricoParticipacaoDTO;
import br.com.senac.migus.dto.MinhaInscricaoDTO;
import br.com.senac.migus.service.InscricaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios/me")
public class MinhasInscricoesController {

    private final InscricaoService inscricoes;

    public MinhasInscricoesController(InscricaoService inscricoes) {
        this.inscricoes = inscricoes;
    }

    @GetMapping("/inscricoes")
    public ResponseEntity<List<MinhaInscricaoDTO>> listar(Principal principal) {
        return ResponseEntity.ok(inscricoes.listarMinhasInscricoes(
                principal == null ? null : principal.getName()));
    }

    @GetMapping("/historico-participacao")
    public ResponseEntity<List<HistoricoParticipacaoDTO>> consultarHistorico(Principal principal) {
        return ResponseEntity.ok(inscricoes.consultarHistoricoParticipacao(
                principal == null ? null : principal.getName()));
    }
}
