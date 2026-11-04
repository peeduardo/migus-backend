package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
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
        var inscricao = new Inscricao("507f1f77bcf86cd799439044", USUARIO_ID, PROGRAMA_ID, "PENDENTE",
                new Date(), null, null, null);
        when(programas.existsById(PROGRAMA_ID)).thenReturn(true);
        when(inscricoes.findByProgramaId(PROGRAMA_ID)).thenReturn(java.util.List.of(inscricao));
        when(usuarios.findAllById(java.util.List.of(USUARIO_ID))).thenReturn(java.util.List.of(voluntario));

        var lista = service.listarInscritos(PROGRAMA_ID, "507f1f77bcf86cd799439033");

        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).nome());
        assertEquals("ana@exemplo.com", lista.get(0).email());
        assertEquals("PENDENTE", lista.get(0).status());
        assertEquals(USUARIO_ID, lista.get(0).usuarioId());
    }

    @Test
    void deveBloquearConsultaDeInscritosSemPermissao() {
        autenticarComoResponsavel("interessado", true);
        var erro = assertThrows(AuthException.class,
                () -> service.listarInscritos(PROGRAMA_ID, "507f1f77bcf86cd799439033"));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        autenticarComoResponsavel("responsavel", false);
        assertThrows(AuthException.class,
                () -> service.listarInscritos(PROGRAMA_ID, "507f1f77bcf86cd799439033"));
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(AuthException.class, () -> service.listarInscritos(PROGRAMA_ID, null)).getStatus());
        verify(inscricoes, never()).findByProgramaId(any());
    }

    @Test
    void deveFalharAoListarInscritosDeProgramaInexistente() {
        autenticarComoResponsavel("responsavel", true);
        when(programas.existsById(PROGRAMA_ID)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> service.listarInscritos(PROGRAMA_ID, "507f1f77bcf86cd799439033"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.listarInscritos("invalido", "507f1f77bcf86cd799439033"));
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
}
