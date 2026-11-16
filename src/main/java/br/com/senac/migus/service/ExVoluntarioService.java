package br.com.senac.migus.service;

import br.com.senac.migus.dto.ExVoluntarioDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ExVoluntarioService {
    public static final String SITUACAO_INATIVO = "INATIVO";
    public static final String SITUACAO_SAIDA = "SAIDA";

    private final UsuariosRepository usuarios;
    private final InscricaoRepository inscricoes;

    public ExVoluntarioService(UsuariosRepository usuarios, InscricaoRepository inscricoes) {
        this.usuarios = usuarios;
        this.inscricoes = inscricoes;
    }

    public List<ExVoluntarioDTO> listar(String nome, String situacao, String usuarioId) {
        exigirResponsavelAtivo(usuarioId);
        String filtroSituacao = normalizarSituacao(situacao);
        String filtroNome = normalizarTexto(nome);

        Set<String> comSaida = new HashSet<>();
        Map<String, Date> ultimaSaida = new HashMap<>();
        for (Inscricao inscricao : inscricoes.findByStatus(InscricaoService.STATUS_SAIDA)) {
            String voluntarioId = inscricao.getVoluntarioId();
            if (voluntarioId == null) {
                continue;
            }
            comSaida.add(voluntarioId);
            Date data = inscricao.getDataSaida();
            Date atual = ultimaSaida.get(voluntarioId);
            if (data != null && (atual == null || data.after(atual))) {
                ultimaSaida.put(voluntarioId, data);
            }
        }

        Map<String, Usuarios> candidatos = new LinkedHashMap<>();
        for (Usuarios usuario : usuarios.findByAtivoFalse()) {
            candidatos.put(usuario.getId(), usuario);
        }
        if (!comSaida.isEmpty()) {
            for (Usuarios usuario : usuarios.findAllById(comSaida)) {
                candidatos.putIfAbsent(usuario.getId(), usuario);
            }
        }

        return candidatos.values().stream()
                .filter(this::ehVoluntario)
                .filter(usuario -> SITUACAO_INATIVO.equals(filtroSituacao)
                        ? !Boolean.TRUE.equals(usuario.getAtivo())
                        : !SITUACAO_SAIDA.equals(filtroSituacao) || comSaida.contains(usuario.getId()))
                .filter(usuario -> filtroNome.isEmpty() || normalizarTexto(usuario.getNome()).contains(filtroNome))
                .sorted(Comparator.comparing(usuario -> normalizarTexto(usuario.getNome())))
                .map(usuario -> ExVoluntarioDTO.of(usuario, comSaida.contains(usuario.getId()),
                        ultimaSaida.get(usuario.getId())))
                .toList();
    }

    private void exigirResponsavelAtivo(String usuarioId) {
        if (usuarioId == null) {
            throw new AuthException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var responsavel = usuarios.findById(usuarioId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado", HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(responsavel.getAtivo())
                || !"RESPONSAVEL".equals(responsavel.getTipoUsuario())) {
            throw new AuthException("Somente um responsável ativo pode consultar ex-voluntários", HttpStatus.FORBIDDEN);
        }
    }

    private boolean ehVoluntario(Usuarios usuario) {
        String perfil = usuario.getTipoUsuario();
        return "interessado".equalsIgnoreCase(perfil) || "VOLUNTARIO".equalsIgnoreCase(perfil);
    }

    private String normalizarSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return null;
        }
        String valor = situacao.trim().toUpperCase(Locale.ROOT);
        if (!SITUACAO_INATIVO.equals(valor) && !SITUACAO_SAIDA.equals(valor)) {
            throw new IllegalArgumentException("Situação inválida. Use INATIVO ou SAIDA");
        }
        return valor;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
