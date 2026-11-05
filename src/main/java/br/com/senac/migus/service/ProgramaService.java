package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;

import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;

@Service
public class ProgramaService {
    private final ProgramaRepository programas;
    private final UsuariosRepository usuarios;

    public ProgramaService(ProgramaRepository programas, UsuariosRepository usuarios) {
        this.programas = programas;
        this.usuarios = usuarios;
    }

    public Programa cadastrar(ProgramaRequestDTO pedido, String usuarioId) {
        if (usuarioId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var responsavel = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(responsavel.getAtivo())
                || !"RESPONSAVEL".equals(responsavel.getTipoUsuario())) {
            throw new AuthException("Somente um responsável ativo pode cadastrar programas", HttpStatus.FORBIDDEN);
        }
        if (!ObjectId.isValid(responsavel.getId())) {
            throw new AuthException("O responsável deve possuir um identificador MongoDB válido", HttpStatus.FORBIDDEN);
        }
        var programa = new Programa(new ObjectId().toHexString(), pedido.nome(), pedido.descricao(),
                pedido.categoria(), pedido.diasDisponiveis(), pedido.horarioInicio(), pedido.horarioFim(),
                pedido.ativo(), responsavel.getId(), new Date(), null);
        return programas.save(programa);
    }

    public List<Programa> listarAtivos() {
        return programas.findByAtivoTrue();
    }

    public Programa alterarDisponibilidade(String programaId, Boolean ativo, String usuarioId) {

        if (usuarioId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }

        if (ativo == null) {
            throw new AuthException("O estado de disponibilidade é obrigatório", HttpStatus.BAD_REQUEST);
        }

        var responsavel = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));

        if (!Boolean.TRUE.equals(responsavel.getAtivo())
                || !"RESPONSAVEL".equals(responsavel.getTipoUsuario())) {
            throw new AuthException("Somente um responsável ativo pode alterar programas", HttpStatus.FORBIDDEN);
        }

        var programa = programas.findByIdAndResponsavelId(programaId, responsavel.getId())
                .orElseThrow(
                        () -> new AuthException("Programa não encontrado ou não autorizado", HttpStatus.NOT_FOUND));

        var programaAtualizado = new Programa(
                programa.getId(),
                programa.getNome(),
                programa.getDescricao(),
                programa.getCategoria(),
                programa.getDiasDisponiveis(),
                programa.getHorarioInicio(),
                programa.getHorarioFim(),
                ativo,
                programa.getResponsavelId(),
                programa.getDataCadastro(),
                new Date());

        return programas.save(programaAtualizado);
    }

    public List<Programa> listarMeusProgramas(String usuarioId) {
        if (usuarioId == null) {
            throw new AuthException(
                    "Autenticação necessária",
                    HttpStatus.UNAUTHORIZED);
        }

        var responsavel = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException(
                        "Usuário não encontrado",
                        HttpStatus.FORBIDDEN));

        if (!Boolean.TRUE.equals(responsavel.getAtivo())
                || !"RESPONSAVEL".equals(responsavel.getTipoUsuario())) {
            throw new AuthException(
                    "Somente um responsável ativo pode consultar seus programas",
                    HttpStatus.FORBIDDEN);
        }

        return programas.findByResponsavelId(responsavel.getId());
    }
}
