package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InscricaoServiceTest {
    private static final String USUARIO_ID = "507f1f77bcf86cd799439011";
    private static final String PROGRAMA_ID = "507f1f77bcf86cd799439022";

    private InscricaoRepository inscricoes;
    private ProgramaRepository programas;
    private UsuariosRepository usuarios;
    private InscricaoService service;

    @BeforeEach
    void preparar() {
        inscricoes = mock(InscricaoRepository.class);
        programas = mock(ProgramaRepository.class);
        usuarios = mock(UsuariosRepository.class);
        service = new InscricaoService(inscricoes, programas, usuarios);
        var usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setAtivo(true);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(programa(true)));
        when(inscricoes.save(any(Inscricao.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Programa programa(boolean ativo) {
        return new Programa(PROGRAMA_ID, "Apoio", "Descrição", null, null, "09:00", "12:00",
                ativo, USUARIO_ID, new Date(), null);
    }

    @Test
    void deveCriarInscricaoPendente() {
        var inscricao = service.inscrever(PROGRAMA_ID, USUARIO_ID);

        assertEquals(USUARIO_ID, inscricao.getVoluntarioId());
        assertEquals(PROGRAMA_ID, inscricao.getProgramaId());
        assertEquals("PENDENTE", inscricao.getStatus());
        assertNotNull(inscricao.getDataInscricao());
        assertNull(inscricao.getDataAprovacao());
        assertNull(inscricao.getDataRecusa());
        assertNull(inscricao.getDataAtualizacao());
        verify(inscricoes).save(inscricao);
    }

    @Test
    void deveExigirAutenticacao() {
        var erro = assertThrows(AuthException.class, () -> service.inscrever(PROGRAMA_ID, null));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
    }

    @Test
    void deveBloquearUsuarioInativo() {
        var usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setAtivo(false);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        var erro = assertThrows(AuthException.class, () -> service.inscrever(PROGRAMA_ID, USUARIO_ID));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveFalharQuandoProgramaNaoExiste() {
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.inscrever(PROGRAMA_ID, USUARIO_ID));
        assertThrows(ResourceNotFoundException.class, () -> service.inscrever("invalido", USUARIO_ID));
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveRejeitarProgramaInativo() {
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(programa(false)));
        assertThrows(IllegalArgumentException.class, () -> service.inscrever(PROGRAMA_ID, USUARIO_ID));
        verify(inscricoes, never()).save(any());
    }

    private void autenticarComoResponsavel(String perfil, boolean ativo) {
        var responsavel = new Usuarios();
        responsavel.setId("507f1f77bcf86cd799439033");
        responsavel.setTipoUsuario(perfil);
        responsavel.setAtivo(ativo);
        when(usuarios.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
    }

    @Test
    void deveListarInscritosDoProgramaParaResponsavel() {
        autenticarComoResponsavel("responsavel", true);
        var voluntario = new Usuarios();
        voluntario.setId(USUARIO_ID);
        voluntario.setNome("Ana");
        voluntario.setEmail("ana@exemplo.com");
        voluntario.setTelefone("11999999999");
        var inscricao = new Inscricao("507f1f77bcf86cd799439044", USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        programaDoResponsavel(RESPONSAVEL_ID);
        when(inscricoes.findByProgramaIdAndStatus(PROGRAMA_ID, "APROVADA")).thenReturn(java.util.List.of(inscricao));
        when(usuarios.findAllById(java.util.List.of(USUARIO_ID))).thenReturn(java.util.List.of(voluntario));

        var lista = service.listarInscritos(PROGRAMA_ID, "APROVADA", "507f1f77bcf86cd799439033");

        // Verifica que retornou apenas voluntários do programa selecionado
        assertEquals(1, lista.size());
        assertEquals(USUARIO_ID, lista.get(0).usuarioId());
        // Confirma que os dados correspondem aos registros do MongoDB
        assertEquals("Ana", lista.get(0).nome());
        assertEquals("ana@exemplo.com", lista.get(0).email());
        assertEquals("11999999999", lista.get(0).telefone());
        assertEquals("APROVADA", lista.get(0).status());
        assertEquals("507f1f77bcf86cd799439044", lista.get(0).inscricaoId());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaVoluntariosVinculados() {
        autenticarComoResponsavel("responsavel", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        when(inscricoes.findByProgramaId(PROGRAMA_ID)).thenReturn(java.util.List.of());

        var lista = service.listarInscritos(PROGRAMA_ID, null, "507f1f77bcf86cd799439033");

        assertEquals(0, lista.size());
    }

    @Test
    void deveListarApenasInscritosDoProgramaSelecionado() {
        autenticarComoResponsavel("responsavel", true);
        var outroPrograma = "507f1f77bcf86cd799439099";
        var voluntario = new Usuarios();
        voluntario.setId(USUARIO_ID);
        voluntario.setNome("Ana");
        // inscrição no PROGRAMA_ID
        var inscricaoProg1 = new Inscricao("507f1f77bcf86cd799439044", USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        programaDoResponsavel(RESPONSAVEL_ID);
        when(inscricoes.findByProgramaIdAndStatus(PROGRAMA_ID, "APROVADA"))
                .thenReturn(java.util.List.of(inscricaoProg1)); // só retorna do programa selecionado
        when(usuarios.findAllById(any())).thenReturn(java.util.List.of(voluntario));

        var lista = service.listarInscritos(PROGRAMA_ID, "APROVADA", "507f1f77bcf86cd799439033");

        assertEquals(1, lista.size());
        // confirma que não veio inscrição do outro programa
        assertTrue(lista.stream().noneMatch(i -> i.inscricaoId().equals("507f1f77bcf86cd799439055")));
        verify(inscricoes, never()).findByProgramaIdAndStatus(eq(outroPrograma), any());
    }

    @Test
    void deveBloquearConsultaDeInscritosSemPermissao() {
        autenticarComoResponsavel("interessado", true);
        var erro = assertThrows(AuthException.class,
                () -> service.listarInscritos(PROGRAMA_ID, null, "507f1f77bcf86cd799439033"));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        autenticarComoResponsavel("responsavel", false);
        assertThrows(AuthException.class,
                () -> service.listarInscritos(PROGRAMA_ID, null, "507f1f77bcf86cd799439033"));
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(AuthException.class, () -> service.listarInscritos(PROGRAMA_ID, null, null)).getStatus());
        verify(inscricoes, never()).findByProgramaIdAndStatus(any(), any());
    }

    @Test
    void deveFalharAoListarInscritosDeProgramaInexistente() {
        autenticarComoResponsavel("responsavel", true);
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.listarInscritos(PROGRAMA_ID, null, "507f1f77bcf86cd799439033"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.listarInscritos("invalido", null, "507f1f77bcf86cd799439033"));
    }

    private static final String RESPONSAVEL_ID = "507f1f77bcf86cd799439033";
    private static final String INSCRICAO_ID = "507f1f77bcf86cd799439044";

    private Inscricao inscricaoPendente() {
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        return inscricao;
    }

    private void programaDoResponsavel(String responsavelId) {
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(new Programa(PROGRAMA_ID, "Apoio",
                "Descrição", null, null, "09:00", "12:00", true, responsavelId, new Date(), null)));
    }

    @Test
    void deveAprovarInscricaoPendente() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        inscricaoPendente();

        var inscricao = service.aprovar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID);

        assertEquals("APROVADA", inscricao.getStatus());
        assertNotNull(inscricao.getDataAprovacao());
        assertNotNull(inscricao.getDataAtualizacao());
        assertNull(inscricao.getDataRecusa());
        verify(inscricoes).save(inscricao);
    }

    @Test
    void deveRecusarInscricaoPendente() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        inscricaoPendente();

        var inscricao = service.recusar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID);

        assertEquals("RECUSADA", inscricao.getStatus());
        assertNotNull(inscricao.getDataRecusa());
        assertNotNull(inscricao.getDataAtualizacao());
        assertNull(inscricao.getDataAprovacao());
        verify(inscricoes).save(inscricao);
        verify(inscricoes, never()).delete(any(Inscricao.class));
    }

    @Test
    void deveBloquearDecisaoDeQuemNaoEhResponsavelAtivo() {
        autenticarComoResponsavel("interessado", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        inscricaoPendente();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.aprovar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(AuthException.class,
                () -> service.recusar(PROGRAMA_ID, INSCRICAO_ID, null)).getStatus());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveBloquearDecisaoDeResponsavelDeOutroPrograma() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel("507f1f77bcf86cd799439099");
        inscricaoPendente();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.aprovar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID)).getStatus());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveRejeitarDecisaoSobreInscricaoJaAnalisada() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        inscricaoPendente().aprovar(new Date());
        assertEquals(HttpStatus.CONFLICT, assertThrows(AuthException.class,
                () -> service.recusar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID)).getStatus());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveFalharQuandoInscricaoNaoExisteOuEhDeOutroPrograma() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel(RESPONSAVEL_ID);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.aprovar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID));
        assertThrows(ResourceNotFoundException.class,
                () -> service.aprovar(PROGRAMA_ID, "invalido", RESPONSAVEL_ID));
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(new Inscricao(INSCRICAO_ID, USUARIO_ID,
                "507f1f77bcf86cd799439055", "PENDENTE", new Date(), null, null, null)));
        assertThrows(ResourceNotFoundException.class,
                () -> service.aprovar(PROGRAMA_ID, INSCRICAO_ID, RESPONSAVEL_ID));
        verify(inscricoes, never()).save(any());
    }


    @Test
    void deveRejeitarInscricaoDuplicada() {
        when(inscricoes.existsByVoluntarioIdAndProgramaId(USUARIO_ID, PROGRAMA_ID)).thenReturn(true);
        var erro = assertThrows(AuthException.class, () -> service.inscrever(PROGRAMA_ID, USUARIO_ID));
        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        verify(inscricoes, never()).save(any());
    }

    // --- transferir branches ---

    @Test
    void deveBloquearTransferenciaDeInscricaoNaoAprovada() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));

        assertEquals(HttpStatus.CONFLICT, assertThrows(AuthException.class,
                () -> service.transferir(INSCRICAO_ID, "destino-id", RESPONSAVEL_ID)).getStatus());
    }

    @Test
    void deveBloquearTransferenciaDeResponsavelDeOutroPrograma() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                new Programa(PROGRAMA_ID, "P", "D", null, null, "09:00", "18:00", true, "outro-resp", new Date(), null)));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.transferir(INSCRICAO_ID, "destino-id", RESPONSAVEL_ID)).getStatus());
    }

    @Test
    void deveBloquearTransferenciaParaMesmoPrograma() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                new Programa(PROGRAMA_ID, "P", "D", null, null, "09:00", "18:00", true, RESPONSAVEL_ID, new Date(), null)));

        assertThrows(IllegalArgumentException.class,
                () -> service.transferir(INSCRICAO_ID, PROGRAMA_ID, RESPONSAVEL_ID));
    }

    @Test
    void deveBloquearTransferenciaParaProgramaInativo() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        String DESTINO_ID = "507f1f77bcf86cd799439099";
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                new Programa(PROGRAMA_ID, "P", "D", null, null, "09:00", "18:00", true, RESPONSAVEL_ID, new Date(), null)));
        when(programas.findById(DESTINO_ID)).thenReturn(Optional.of(
                new Programa(DESTINO_ID, "P2", "D2", null, null, "09:00", "18:00", false, "resp2", new Date(), null)));

        assertThrows(IllegalArgumentException.class,
                () -> service.transferir(INSCRICAO_ID, DESTINO_ID, RESPONSAVEL_ID));
    }

    @Test
    void deveBloquearTransferenciaSeVoluntarioJaInscritoNoDestino() {
        autenticarComoResponsavel("RESPONSAVEL", true);
        String DESTINO_ID = "507f1f77bcf86cd799439099";
        var inscricao = new Inscricao(INSCRICAO_ID, USUARIO_ID, PROGRAMA_ID, "APROVADA",
                new Date(), new Date(), null, null);
        when(inscricoes.findById(INSCRICAO_ID)).thenReturn(Optional.of(inscricao));
        when(programas.findById(PROGRAMA_ID)).thenReturn(Optional.of(
                new Programa(PROGRAMA_ID, "P", "D", null, null, "09:00", "18:00", true, RESPONSAVEL_ID, new Date(), null)));
        when(programas.findById(DESTINO_ID)).thenReturn(Optional.of(
                new Programa(DESTINO_ID, "P2", "D2", null, null, "09:00", "18:00", true, "resp2", new Date(), null)));
        when(inscricoes.existsByVoluntarioIdAndProgramaId(USUARIO_ID, DESTINO_ID)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(AuthException.class,
                () -> service.transferir(INSCRICAO_ID, DESTINO_ID, RESPONSAVEL_ID)).getStatus());
    }

    @Test
    void deveConsultarMinhasInscricoesComProgramaDataEStatus() {
        var data = new Date(1760000000000L);
        var inscricao = new Inscricao("inscricao-1", USUARIO_ID, PROGRAMA_ID, "PENDENTE",
                data, null, null, null);
        when(inscricoes.findByVoluntarioId(USUARIO_ID)).thenReturn(java.util.List.of(inscricao));
        when(programas.findAllById(java.util.List.of(PROGRAMA_ID))).thenReturn(java.util.List.of(programa(true)));

        var lista = service.listarMinhasInscricoes(USUARIO_ID);

        assertEquals(1, lista.size());
        assertEquals("inscricao-1", lista.get(0).inscricaoId());
        assertEquals(PROGRAMA_ID, lista.get(0).programaId());
        assertEquals("Apoio", lista.get(0).programaNome());
        assertEquals(data, lista.get(0).dataInscricao());
        assertEquals("PENDENTE", lista.get(0).status());
        verify(inscricoes, never()).save(any());
    }

    @Test
    void deveRetornarListaVaziaSemConsultarProgramas() {
        when(inscricoes.findByVoluntarioId(USUARIO_ID)).thenReturn(java.util.List.of());
        assertTrue(service.listarMinhasInscricoes(USUARIO_ID).isEmpty());
        verify(programas, never()).findAllById(any());
    }

    @ParameterizedTest
    @CsvSource({"PENDENTE,true", "APROVADA,false", "RECUSADA,true"})
    void devePreservarStatusEIncluirProgramaInativo(String status, boolean ativo) {
        var inscricao = new Inscricao("inscricao-1", USUARIO_ID, PROGRAMA_ID, status,
                new Date(), null, null, null);
        when(inscricoes.findByVoluntarioId(USUARIO_ID)).thenReturn(java.util.List.of(inscricao));
        when(programas.findAllById(java.util.List.of(PROGRAMA_ID))).thenReturn(java.util.List.of(programa(ativo)));
        var lista = service.listarMinhasInscricoes(USUARIO_ID);
        assertEquals(status, lista.get(0).status());
        assertEquals("Apoio", lista.get(0).programaNome());
        verify(programas, never()).findByAtivoTrue();
    }

    @Test
    void deveManterInscricaoQuandoProgramaNaoExiste() {
        var inscricao = new Inscricao("inscricao-1", USUARIO_ID, PROGRAMA_ID, "RECUSADA",
                new Date(), null, null, null);
        when(inscricoes.findByVoluntarioId(USUARIO_ID)).thenReturn(java.util.List.of(inscricao));
        when(programas.findAllById(java.util.List.of(PROGRAMA_ID))).thenReturn(java.util.List.of());
        var lista = service.listarMinhasInscricoes(USUARIO_ID);
        assertEquals(1, lista.size());
        assertEquals(PROGRAMA_ID, lista.get(0).programaId());
        assertNull(lista.get(0).programaNome());
        assertEquals("RECUSADA", lista.get(0).status());
        verify(inscricoes, never()).delete(any(Inscricao.class));
    }

    @Test
    void deveExigirUsuarioExistenteEAtivoNaConsultaPropria() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(AuthException.class,
                () -> service.listarMinhasInscricoes(null)).getStatus());
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.listarMinhasInscricoes(USUARIO_ID)).getStatus());
        var usuario = new Usuarios();
        usuario.setAtivo(false);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.listarMinhasInscricoes(USUARIO_ID)).getStatus());
        usuario.setAtivo(null);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AuthException.class,
                () -> service.listarMinhasInscricoes(USUARIO_ID)).getStatus());
        verify(inscricoes, never()).findByVoluntarioId(any());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "PENDENTE")
    void deveBloquearConsultaDeInscritosDeOutroResponsavel(String status) {
        autenticarComoResponsavel("RESPONSAVEL", true);
        programaDoResponsavel(USUARIO_ID);
        var erro = assertThrows(AuthException.class,
                () -> service.listarInscritos(PROGRAMA_ID, status, RESPONSAVEL_ID));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verifyNoInteractions(inscricoes);
        verify(usuarios, never()).findAllById(any());
    }
}
