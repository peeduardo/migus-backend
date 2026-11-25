package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.dto.DadosPessoaisExportDTO;
import br.com.senac.migus.dto.MinhaInscricaoDTO;
import br.com.senac.migus.dto.UsuarioDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UsuariosService {

    private final UsuariosRepository usuariosRepository;
    private final InscricaoRepository inscricaoRepository;
    private final ProgramaRepository programaRepository;

    public UsuariosService(UsuariosRepository usuariosRepository,
                           InscricaoRepository inscricaoRepository,
                           ProgramaRepository programaRepository) {
        this.usuariosRepository = usuariosRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.programaRepository = programaRepository;
    }

    public void desativarMinhaConta(String id) {
        var usuario = usuariosRepository.findById(id)
                .filter(conta -> Boolean.TRUE.equals(conta.getAtivo()))
                .orElseThrow(this::contaIndisponivel);
        String perfil = usuario.getTipoUsuario();
        if (!"interessado".equalsIgnoreCase(perfil) && !"VOLUNTARIO".equalsIgnoreCase(perfil)) {
            throw new AuthException("Este perfil não pode desativar a própria conta", HttpStatus.FORBIDDEN);
        }
        if (usuariosRepository.desativarParticipante(id, perfil, LocalDate.now()) == 0) {
            throw contaIndisponivel();
        }
    }

    public UsuarioResponseDTO inativarVoluntario(String voluntarioId, String usuarioAutenticadoId) {
        if (usuarioAutenticadoId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var responsavel = usuariosRepository.findById(usuarioAutenticadoId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(responsavel.getAtivo())
                || !"RESPONSAVEL".equals(responsavel.getTipoUsuario())) {
            throw new AuthException("Somente um responsável ativo pode inativar cadastros", HttpStatus.FORBIDDEN);
        }
        Usuarios voluntario = usuariosRepository.findById(voluntarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Voluntário não encontrado com o ID: " + voluntarioId));
        String perfil = voluntario.getTipoUsuario();
        if (!"interessado".equalsIgnoreCase(perfil) && !"VOLUNTARIO".equalsIgnoreCase(perfil)) {
            throw new AuthException("Somente cadastros de voluntários podem ser inativados", HttpStatus.FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(voluntario.getAtivo())) {
            throw new AuthException("O cadastro já está inativo", HttpStatus.CONFLICT);
        }
        LocalDate hoje = LocalDate.now();
        if (usuariosRepository.desativarParticipante(voluntarioId, perfil, hoje) == 0) {
            throw new AuthException("O cadastro já está inativo", HttpStatus.CONFLICT);
        }
        voluntario.setAtivo(false);
        voluntario.setDataAtualizacao(hoje);
        return new UsuarioResponseDTO("Cadastro inativado com sucesso!", UsuarioDTO.fromEntity(voluntario));
    }

    private AuthException contaIndisponivel() {
        return new AuthException("A conta não está disponível para esta sessão", HttpStatus.UNAUTHORIZED);
    }

    public UsuarioDTO getMeuPerfil(String userId) {
        Usuarios usuario = usuariosRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return UsuarioDTO.fromEntity(usuario);
    }

    public UsuarioResponseDTO atualizar(String id, UsuarioUpdateRequestDTO dto, String usuarioAutenticadoId) {
        if (usuarioAutenticadoId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        if (!usuarioAutenticadoId.equals(id)) {
            throw new AuthException("Você só pode atualizar sua própria conta", HttpStatus.FORBIDDEN);
        }
        if (dto.tipoUsuario() != null || dto.ativo() != null) {
            throw new AuthException("Não é permitido alterar perfil ou status nesta operação", HttpStatus.FORBIDDEN);
        }
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        if (dto.email() != null && !dto.email().equalsIgnoreCase(usuario.getEmail())) {
            usuariosRepository.findByEmail(dto.email()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new IllegalArgumentException("O e-mail informado já está em uso por outro usuário");
                }
            });
        }

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        if (dto.telefone() != null) {
            usuario.setTelefone(dto.telefone());
        }
        if (dto.dataNascimento() != null) {
            if (dto.dataNascimento().isAfter(LocalDate.now().minusYears(17))) {
                throw new AuthException("É necessário ter no mínimo 17 anos para atualizar o cadastro",
                        HttpStatus.BAD_REQUEST);
            }
            usuario.setDataNascimento(dto.dataNascimento());
        }
        usuario.setDataAtualizacao(LocalDate.now());

        long alterados = usuariosRepository.atualizarDadosPessoais(id, usuario.getNome(), usuario.getEmail(),
                usuario.getTelefone(), usuario.getDataNascimento(), usuario.getDataAtualizacao());
        // O Mongo também retorna zero quando os dados já eram iguais.
        if (alterados == 0 && !usuariosRepository.existsByIdAndAtivoTrue(id)) {
            throw contaIndisponivel();
        }
        return UsuarioResponseDTO.comSucesso(usuario);
    }

    public DadosPessoaisExportDTO exportarDados(String userId) {
        Usuarios usuario = usuariosRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        List<Inscricao> inscricoesList = inscricaoRepository.findByVoluntarioId(userId);
        Map<String, Programa> programasPorId = programaRepository
                .findAllById(inscricoesList.stream().map(Inscricao::getProgramaId).distinct().toList())
                .stream().collect(Collectors.toMap(Programa::getId, Function.identity()));
        List<MinhaInscricaoDTO> inscricoesDTO = inscricoesList.stream()
                .map(i -> MinhaInscricaoDTO.of(i, programasPorId.get(i.getProgramaId())))
                .toList();
        return DadosPessoaisExportDTO.of(usuario, inscricoesDTO);
    }

    public UsuarioResponseDTO anonimizarMinhaConta(String userId) {
        Usuarios usuario = usuariosRepository.findById(userId)
                .orElseThrow(this::contaIndisponivel);
        String emailAnonimo = "removido-" + UUID.randomUUID() + "@anonimizado.invalid";
        String cpfAnonimo = "00000000000";
        long alterados = usuariosRepository.anonimizarUsuario(userId, emailAnonimo, cpfAnonimo, LocalDate.now());
        if (alterados == 0) {
            throw contaIndisponivel();
        }
        usuario.setNome("Usuário Removido");
        usuario.setEmail(emailAnonimo);
        usuario.setCpf(cpfAnonimo);
        usuario.setTelefone(null);
        usuario.setDataNascimento(null);
        usuario.setAtivo(false);
        return new UsuarioResponseDTO("Dados pessoais anonimizados com sucesso.", UsuarioDTO.fromEntity(usuario));
    }
}
