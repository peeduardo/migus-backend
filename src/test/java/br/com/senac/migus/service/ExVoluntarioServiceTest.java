package br.com.senac.migus.service;

import br.com.senac.migus.dto.ExVoluntarioDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExVoluntarioServiceTest {
    private UsuariosRepository usuarios;
    private InscricaoRepository inscricoes;
    private ExVoluntarioService service;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuariosRepository.class);
        inscricoes = mock(InscricaoRepository.class);
        service = new ExVoluntarioService(usuarios, inscricoes);
        when(usuarios.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "Responsável", "RESPONSAVEL", true)));
        when(usuarios.findByAtivoFalse()).thenReturn(List.of());
        when(inscricoes.findByStatus(InscricaoService.STATUS_SAIDA)).thenReturn(List.of());
    }

    private Usuarios conta(String id, String nome, String perfil, boolean ativo) {
        var usuario = new Usuarios(nome, id + "@exemplo.invalid", "hash", "12345678901", "11999990000",
                LocalDate.of(2000, 1, 1), perfil, ativo, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        usuario.setId(id);
        return usuario;
    }

    private Inscricao saida(String voluntarioId, Date data) {
        var inscricao = new Inscricao("insc-" + voluntarioId + data, voluntarioId, "prog-1", "APROVADA",
                new Date(0), new Date(0), null, null);
        inscricao.sair(data);
        return inscricao;
    }

    @SuppressWarnings("unchecked")
    private void comSaidas(List<Inscricao> lista, Usuarios... donos) {
        when(inscricoes.findByStatus(InscricaoService.STATUS_SAIDA)).thenReturn(lista);
        when(usuarios.findAllById(any(Iterable.class))).thenAnswer(chamada -> {
            Collection<String> ids = (Collection<String>) chamada.getArgument(0);
            return List.of(donos).stream().filter(u -> ids.contains(u.getId())).toList();
        });
    }

    @Test
    void listaCadastrosInativosESaidasRegistradasComOsDadosArmazenados() {
        var inativo = conta("u-ana", "Ana", "interessado", false);
        var saiu = conta("u-bruno", "Bruno", "VOLUNTARIO", true);
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(inativo));
        comSaidas(List.of(saida("u-bruno", new Date(1000))), saiu);

        var lista = service.listar(null, null, "resp-1");

        assertEquals(List.of("u-ana", "u-bruno"), lista.stream().map(ExVoluntarioDTO::id).toList());
        var ana = lista.get(0);
        assertEquals("Ana", ana.nome());
        assertEquals("u-ana@exemplo.invalid", ana.email());
        assertEquals("11999990000", ana.telefone());
        assertEquals("interessado", ana.tipoUsuario());
        assertFalse(ana.cadastroAtivo());
        assertFalse(ana.saidaRegistrada());
        assertNull(ana.ultimaSaida());
        assertEquals(LocalDate.of(2026, 9, 1), ana.dataCadastro());
        assertEquals(LocalDate.of(2026, 10, 1), ana.dataAtualizacao());
        var bruno = lista.get(1);
        assertTrue(bruno.cadastroAtivo());
        assertTrue(bruno.saidaRegistrada());
        assertEquals(new Date(1000), bruno.ultimaSaida());
    }

    @Test
    void cadastroInativoComSaidaApareceUmaVezComAUltimaSaida() {
        var carla = conta("u-carla", "Carla", "VOLUNTARIO", false);
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(carla));
        comSaidas(List.of(saida("u-carla", new Date(1000)), saida("u-carla", new Date(5000)),
                saida("u-carla", null)), carla);

        var lista = service.listar(null, null, "resp-1");

        assertEquals(1, lista.size());
        assertTrue(lista.get(0).saidaRegistrada());
        assertFalse(lista.get(0).cadastroAtivo());
        assertEquals(new Date(5000), lista.get(0).ultimaSaida());
    }

    @Test
    void naoListaQuemNaoEhVoluntarioNemConsultaUsuariosSemSaida() {
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(conta("resp-2", "Outro responsável", "RESPONSAVEL", false)));

        assertTrue(service.listar(null, null, "resp-1").isEmpty());
        verify(usuarios, never()).findAllById(any());
    }

    @Test
    void retornaListaVaziaQuandoNaoHaExVoluntarios() {
        assertEquals(List.of(), service.listar("", "", "resp-1"));
    }

    @Test
    void filtraPorNomeSemDiferenciarMaiusculasNemAcentos() {
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(
                conta("u-joao", "João Silva", "VOLUNTARIO", false),
                conta("u-maria", "Maria Souza", "VOLUNTARIO", false)));

        assertEquals(List.of("u-joao"), service.listar("  joao ", null, "resp-1").stream().map(ExVoluntarioDTO::id).toList());
        assertEquals(List.of("u-maria"), service.listar("SOUZA", null, "resp-1").stream().map(ExVoluntarioDTO::id).toList());
        assertTrue(service.listar("Pedro", null, "resp-1").isEmpty());
    }

    @Test
    void ordenaPorNome() {
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(
                conta("u-3", "Zeca", "VOLUNTARIO", false),
                conta("u-1", "Álvaro", "VOLUNTARIO", false),
                conta("u-2", "bia", "interessado", false)));

        assertEquals(List.of("Álvaro", "bia", "Zeca"),
                service.listar(null, null, "resp-1").stream().map(ExVoluntarioDTO::nome).toList());
    }

    @Test
    void filtraPorSituacao() {
        var inativo = conta("u-ana", "Ana", "interessado", false);
        var saiu = conta("u-bruno", "Bruno", "VOLUNTARIO", true);
        when(usuarios.findByAtivoFalse()).thenReturn(List.of(inativo));
        comSaidas(List.of(saida("u-bruno", new Date(1000))), saiu);

        assertEquals(List.of("u-ana"), service.listar(null, "inativo", "resp-1").stream().map(ExVoluntarioDTO::id).toList());
        assertEquals(List.of("u-bruno"), service.listar(null, "SAIDA", "resp-1").stream().map(ExVoluntarioDTO::id).toList());
    }

    @Test
    void rejeitaSituacaoInvalida() {
        var erro = assertThrows(IllegalArgumentException.class, () -> service.listar(null, "ATIVO", "resp-1"));
        assertEquals("Situação inválida. Use INATIVO ou SAIDA", erro.getMessage());
    }

    @Test
    void exigeAutenticacao() {
        var erro = assertThrows(AuthException.class, () -> service.listar(null, null, null));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
        verifyNoInteractions(inscricoes);
    }

    @Test
    void bloqueiaSolicitanteInexistente() {
        when(usuarios.findById("sumiu")).thenReturn(Optional.empty());
        var erro = assertThrows(AuthException.class, () -> service.listar(null, null, "sumiu"));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verifyNoInteractions(inscricoes);
    }

    @ParameterizedTest
    @CsvSource({"interessado,true", "VOLUNTARIO,true", "RESPONSAVEL,false"})
    void bloqueiaUsuarioSemAutorizacao(String perfil, boolean ativo) {
        when(usuarios.findById("u")).thenReturn(Optional.of(conta("u", "Usuário", perfil, ativo)));

        var erro = assertThrows(AuthException.class, () -> service.listar(null, null, "u"));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        assertEquals("Somente um responsável ativo pode consultar ex-voluntários", erro.getMessage());
        verifyNoInteractions(inscricoes);
        verify(usuarios, never()).findByAtivoFalse();
    }

    @Test
    void consultaOsDonosDasSaidasPeloIdentificador() {
        var saiu = conta("u-bruno", "Bruno", "VOLUNTARIO", true);
        comSaidas(List.of(saida("u-bruno", new Date(1000))), saiu);

        service.listar(null, null, "resp-1");

        verify(usuarios).findAllById(Set.of("u-bruno"));
    }
}
