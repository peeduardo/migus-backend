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
}
