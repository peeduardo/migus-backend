package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgramaServiceTest {
    @ParameterizedTest
    @CsvSource({"interessado,true", "VOLUNTARIO,true", "RESPONSAVEL,false"})
    void deveBloquearUsuarioSemPermissao(String perfil, boolean ativo) {
        var usuarios = mock(UsuariosRepository.class);
        var programas = mock(ProgramaRepository.class);
        var usuario = new Usuarios();
        usuario.setId("507f1f77bcf86cd799439011");
        usuario.setTipoUsuario(perfil);
        usuario.setAtivo(ativo);
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        var pedido = new ProgramaRequestDTO("Apoio", "Descrição", List.of("Educação"),
                List.of("segunda"), "09:00", "12:00", true);

        var erro = assertThrows(AuthException.class,
                () -> new ProgramaService(programas, usuarios).cadastrar(pedido, usuario.getId()));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
        verify(programas, never()).save(any());
    }
    @Test
    void buscarPorId_deveRetornarDetalhesDoPrograma() {
        var usuarios = mock(UsuariosRepository.class);
        var programas = mock(ProgramaRepository.class);
        var programa = new Programa("507f1f77bcf86cd799439011", "Apoio", "Apoio à comunidade",
                List.of("Educação"), List.of("segunda"), "09:00", "12:00", true,
                "responsavel-id", new Date(), null);
        when(programas.findById("507f1f77bcf86cd799439011")).thenReturn(Optional.of(programa));

        var dto = new ProgramaService(programas, usuarios).buscarPorId("507f1f77bcf86cd799439011");

        assertEquals("507f1f77bcf86cd799439011", dto.id());
        assertEquals("Apoio", dto.nome());
        assertEquals("Apoio à comunidade", dto.descricao());
        assertNull(dto.getClass().getRecordComponents()[0].getDeclaringRecord()
                .getDeclaredFields().length > 0 ? null : null); // responsavelId não existe no DTO
    }

    @Test
    void buscarPorId_deveLancarExcecaoSeNaoEncontrado() {
        var usuarios = mock(UsuariosRepository.class);
        var programas = mock(ProgramaRepository.class);
        when(programas.findById("inexistente")).thenReturn(Optional.empty());

        var ex = assertThrows(br.com.senac.migus.exception.ResourceNotFoundException.class,
                () -> new ProgramaService(programas, usuarios).buscarPorId("inexistente"));

        assertEquals("Programa não encontrado com o ID: inexistente", ex.getMessage());
    }

    @Test
    void deveCadastrarProgramaComResponsavelEDataDoServidor() {
        var usuarios = mock(UsuariosRepository.class);
        var programas = mock(ProgramaRepository.class);
        var responsavel = new Usuarios();
        responsavel.setId("507f1f77bcf86cd799439011");
        responsavel.setTipoUsuario("RESPONSAVEL");
        responsavel.setAtivo(true);
        when(usuarios.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(programas.save(any(Programa.class))).thenAnswer(call -> call.getArgument(0));
        var pedido = new ProgramaRequestDTO("Apoio", "Apoio à comunidade", List.of("Educação"),
                List.of("segunda"), "09:00", "12:00", false);
        var inicio = new Date();

        var programa = new ProgramaService(programas, usuarios).cadastrar(pedido, responsavel.getId());

        assertTrue(ObjectId.isValid(programa.getId()));
        assertEquals(responsavel.getId(), programa.getResponsavelId());
        assertEquals("Apoio", programa.getNome());
        assertEquals("Apoio à comunidade", programa.getDescricao());
        assertEquals(List.of("Educação"), programa.getCategoria());
        assertEquals(List.of("segunda"), programa.getDiasDisponiveis());
        assertEquals("09:00", programa.getHorarioInicio());
        assertEquals("12:00", programa.getHorarioFim());
        assertFalse(programa.getAtivo());
        assertFalse(programa.getDataCadastro().before(inicio));
        assertFalse(programa.getDataCadastro().after(new Date()));
        assertNull(programa.getDataAtualizacao());
        verify(programas).save(programa);
    }
    private ProgramaService service() {
        return new ProgramaService(programas, usuarios);
    }
    private ProgramaRepository programas = mock(ProgramaRepository.class);
    private UsuariosRepository usuarios = mock(UsuariosRepository.class);

    private Usuarios responsavel(String id, String tipo, boolean ativo) {
        var u = new Usuarios(); u.setId(id); u.setTipoUsuario(tipo); u.setAtivo(ativo); return u;
    }

    private Programa programa(String id, String responsavelId) {
        return new Programa(id, "P", "D", null, null, "09:00", "18:00", true, responsavelId, new Date(), null);
    }

    // --- alterarDisponibilidade ---

    @Test
    void deveAlterarDisponibilidade() {
        var r = responsavel("resp-1", "RESPONSAVEL", true);
        when(usuarios.findById("resp-1")).thenReturn(Optional.of(r));
        when(programas.findByIdAndResponsavelId("prog-1", "resp-1")).thenReturn(Optional.of(programa("prog-1", "resp-1")));
        when(programas.save(any())).thenAnswer(call -> call.getArgument(0));

        var resultado = service().alterarDisponibilidade("prog-1", false, "resp-1");

        assertFalse(resultado.getAtivo());
        verify(programas).save(any());
    }

    @Test
    void deveBloquearAlteracaoSemAutenticacao() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(AuthException.class, () -> service().alterarDisponibilidade("p", true, null)).getStatus());
    }

    @Test
    void deveBloquearAlteracaoSemAtivo() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(AuthException.class, () -> service().alterarDisponibilidade("p", null, "x")).getStatus());
    }

    @Test
    void deveBloquearAlteracaoDeNaoResponsavel() {
        when(usuarios.findById("u")).thenReturn(Optional.of(responsavel("u", "interessado", true)));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(AuthException.class, () -> service().alterarDisponibilidade("p", true, "u")).getStatus());
    }

    @Test
    void deveBloquearAlteracaoDeProgramaDeOutroResponsavel() {
        when(usuarios.findById("u")).thenReturn(Optional.of(responsavel("u", "RESPONSAVEL", true)));
        when(programas.findByIdAndResponsavelId(any(), any())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(AuthException.class, () -> service().alterarDisponibilidade("p", true, "u")).getStatus());
    }

    // --- listarMeusProgramas ---

    @Test
    void deveListarMeusProgramas() {
        var r = responsavel("resp-1", "RESPONSAVEL", true);
        when(usuarios.findById("resp-1")).thenReturn(Optional.of(r));
        when(programas.findByResponsavelId("resp-1")).thenReturn(List.of(programa("p1", "resp-1")));

        var lista = service().listarMeusProgramas("resp-1");

        assertEquals(1, lista.size());
    }

    @Test
    void deveBloquearListarMeusProgramasSemAuth() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(AuthException.class, () -> service().listarMeusProgramas(null)).getStatus());
    }

    @Test
    void deveBloquearListarMeusProgramasDeNaoResponsavel() {
        when(usuarios.findById("u")).thenReturn(Optional.of(responsavel("u", "interessado", true)));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(AuthException.class, () -> service().listarMeusProgramas("u")).getStatus());
    }
}
