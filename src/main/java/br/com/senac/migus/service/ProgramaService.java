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
}
