package br.com.senac.migus.service;

import br.com.senac.migus.dto.InscritoDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InscricaoService {
    public static final String STATUS_PENDENTE = "PENDENTE";

    private final InscricaoRepository inscricoes;
    private final ProgramaRepository programas;
    private final UsuariosRepository usuarios;

    public InscricaoService(InscricaoRepository inscricoes, ProgramaRepository programas,
                            UsuariosRepository usuarios) {
        this.inscricoes = inscricoes;
        this.programas = programas;
        this.usuarios = usuarios;
    }

    public Inscricao inscrever(String programaId, String usuarioId) {
        if (usuarioId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var voluntario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(voluntario.getAtivo())) {
            throw new AuthException("Somente um usuário ativo pode se inscrever em programas", HttpStatus.FORBIDDEN);
        }
        if (!ObjectId.isValid(voluntario.getId())) {
            throw new AuthException("O usuário deve possuir um identificador MongoDB válido", HttpStatus.FORBIDDEN);
        }
        if (!ObjectId.isValid(programaId)) {
            throw new ResourceNotFoundException("Programa não encontrado");
        }
        var programa = programas.findById(programaId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa não encontrado"));
        if (!Boolean.TRUE.equals(programa.getAtivo())) {
            throw new IllegalArgumentException("O programa não está ativo para inscrições");
        }
        if (inscricoes.existsByVoluntarioIdAndProgramaId(voluntario.getId(), programa.getId())) {
            throw new AuthException("Você já está inscrito neste programa", HttpStatus.CONFLICT);
        }
        var inscricao = new Inscricao(new ObjectId().toHexString(), voluntario.getId(), programa.getId(),
                STATUS_PENDENTE, new Date(), null, null, null);
        return inscricoes.save(inscricao);
    }

    public Inscricao aprovar(String programaId, String inscricaoId, String usuarioId) {
        var inscricao = buscarPendenteDoResponsavel(programaId, inscricaoId, usuarioId);
        inscricao.aprovar(new Date());
        return inscricoes.save(inscricao);
    }

    public Inscricao recusar(String programaId, String inscricaoId, String usuarioId) {
        var inscricao = buscarPendenteDoResponsavel(programaId, inscricaoId, usuarioId);
        inscricao.recusar(new Date());
        return inscricoes.save(inscricao);
    }

    private Inscricao buscarPendenteDoResponsavel(String programaId, String inscricaoId, String usuarioId) {
        var responsavel = exigirResponsavelAtivo(usuarioId,
                "Somente um responsável ativo pode aprovar ou recusar inscrições");
        if (!ObjectId.isValid(programaId) || !ObjectId.isValid(inscricaoId)) {
            throw new ResourceNotFoundException("Inscrição não encontrada");
        }
        var programa = programas.findById(programaId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa não encontrado"));
        if (!responsavel.getId().equals(programa.getResponsavelId())) {
            throw new AuthException("Somente o responsável pelo programa pode aprovar ou recusar inscrições",
                    HttpStatus.FORBIDDEN);
        }
        var inscricao = inscricoes.findById(inscricaoId)
                .filter(i -> programaId.equals(i.getProgramaId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));
        if (!STATUS_PENDENTE.equals(inscricao.getStatus())) {
            throw new AuthException("A inscrição já foi analisada", HttpStatus.CONFLICT);
        }
        return inscricao;
    }

    private Usuarios exigirResponsavelAtivo(String usuarioId, String mensagem) {
        if (usuarioId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var solicitante = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(solicitante.getAtivo())
                || !"RESPONSAVEL".equalsIgnoreCase(solicitante.getTipoUsuario())) {
            throw new AuthException(mensagem, HttpStatus.FORBIDDEN);
        }
        return solicitante;
    }

    public static final String STATUS_APROVADA = "APROVADA";
    public static final String STATUS_TRANSFERIDA = "TRANSFERIDA";

    public Inscricao transferir(String inscricaoId, String programaDestinoId, String usuarioId) {
        var responsavel = exigirResponsavelAtivo(usuarioId, "Somente um responsável ativo pode transferir voluntários");

        if (!ObjectId.isValid(inscricaoId)) {
            throw new ResourceNotFoundException("Inscrição não encontrada");
        }
        var inscricaoOrigem = inscricoes.findById(inscricaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada"));

        if (!STATUS_APROVADA.equals(inscricaoOrigem.getStatus())) {
            throw new AuthException("Somente inscrições aprovadas podem ser transferidas", HttpStatus.CONFLICT);
        }

        var programaOrigem = programas.findById(inscricaoOrigem.getProgramaId())
                .orElseThrow(() -> new ResourceNotFoundException("Programa de origem não encontrado"));

        if (!responsavel.getId().equals(programaOrigem.getResponsavelId())) {
            throw new AuthException("Somente o responsável pelo programa de origem pode transferir voluntários",
                    HttpStatus.FORBIDDEN);
        }

        if (inscricaoOrigem.getProgramaId().equals(programaDestinoId)) {
            throw new IllegalArgumentException("O voluntário já está neste programa");
        }

        if (!ObjectId.isValid(programaDestinoId)) {
            throw new ResourceNotFoundException("Programa destino não encontrado");
        }
        var programaDestino = programas.findById(programaDestinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa destino não encontrado"));

        if (!Boolean.TRUE.equals(programaDestino.getAtivo())) {
            throw new IllegalArgumentException("O programa destino não está ativo");
        }

        if (inscricoes.existsByVoluntarioIdAndProgramaId(inscricaoOrigem.getVoluntarioId(), programaDestinoId)) {
            throw new AuthException("O voluntário já está inscrito no programa destino", HttpStatus.CONFLICT);
        }

        var agora = new Date();
        inscricaoOrigem.transferir(agora);
        inscricoes.save(inscricaoOrigem);

        var novaInscricao = new Inscricao(new ObjectId().toHexString(),
                inscricaoOrigem.getVoluntarioId(), programaDestinoId,
                STATUS_APROVADA, agora, agora, null, agora);
        return inscricoes.save(novaInscricao);
    }


    public static final String STATUS_SAIDA = "SAIDA";

    public Inscricao registrarSaida(String programaId, String inscricaoId, String usuarioId) {
        var responsavel = exigirResponsavelAtivo(usuarioId,
                "Somente um responsável ativo pode registrar a saída de um voluntário");

        if (!ObjectId.isValid(programaId) || !ObjectId.isValid(inscricaoId)) {
            throw new ResourceNotFoundException("Inscrição não encontrada");
        }

        var programa = programas.findById(programaId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa não encontrado"));

        if (!responsavel.getId().equals(programa.getResponsavelId())) {
            throw new AuthException("Somente o responsável pelo programa pode registrar saídas",
                    HttpStatus.FORBIDDEN);
        }

        var inscricao = inscricoes.findById(inscricaoId)
                .filter(i -> programaId.equals(i.getProgramaId()))
                .orElseThrow(() -> new ResourceNotFoundException("Voluntário não vinculado a este programa"));

        if (STATUS_SAIDA.equals(inscricao.getStatus())) {
            throw new AuthException("A saída deste voluntário já foi registrada", HttpStatus.CONFLICT);
        }

        if (!STATUS_APROVADA.equals(inscricao.getStatus())) {
            throw new AuthException("Somente voluntários com inscrição aprovada podem ter saída registrada",
                    HttpStatus.CONFLICT);
        }

        inscricao.sair(new Date());
        return inscricoes.save(inscricao);
    }

    public List<InscritoDTO> listarInscritos(String programaId, String status, String usuarioId) {
        exigirResponsavelAtivo(usuarioId, "Somente um responsável ativo pode consultar os inscritos");
        if (!ObjectId.isValid(programaId) || !programas.existsById(programaId)) {
            throw new ResourceNotFoundException("Programa não encontrado");
        }
        var lista = (status != null && !status.isBlank())
                ? inscricoes.findByProgramaIdAndStatus(programaId, status.toUpperCase())
                : inscricoes.findByProgramaId(programaId);
        Map<String, Usuarios> porId = usuarios.findAllById(
                        lista.stream().map(Inscricao::getVoluntarioId).distinct().toList())
                .stream().collect(Collectors.toMap(Usuarios::getId, Function.identity()));
        return lista.stream()
                .map(inscricao -> InscritoDTO.of(inscricao, porId.get(inscricao.getVoluntarioId())))
                .toList();
    }
}
