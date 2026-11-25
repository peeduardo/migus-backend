package br.com.senac.migus.service;

import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.exception.AuthException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DisponibilidadeProgramaTest {
    @ParameterizedTest
    @ValueSource(strings = {"PENDENTE", "APROVADA", "RECUSADA", "CANCELADA", "SAIDA", "TRANSFERIDA", "SUSPENSA"})
    void devePreservarDadosEListagensDuranteOsCiclos(String status) {
        var usuarios = mock(UsuariosRepository.class);
        var programas = mock(ProgramaRepository.class);
        var inscricoes = mock(InscricaoRepository.class);
        String programaId = "507f1f77bcf86cd799439022";
        String participanteId = "507f1f77bcf86cd799439011";
        String responsavelId = "507f1f77bcf86cd799439033";
        var participante = new Usuarios();
        participante.setId(participanteId);
        participante.setAtivo(true);
        participante.setTipoUsuario("INTERESSADO");
        var responsavel = new Usuarios();
        responsavel.setId(responsavelId);
        responsavel.setAtivo(true);
        responsavel.setTipoUsuario("RESPONSAVEL");
        when(usuarios.findById(participanteId)).thenReturn(Optional.of(participante));
        when(usuarios.findById(responsavelId)).thenReturn(Optional.of(responsavel));
        when(usuarios.findAllById(any())).thenReturn(List.of(participante));
        var data = new Date(1000);
        var registro = new Inscricao("inscricao-original", participanteId, programaId, status, data,
                "APROVADA".equals(status) ? data : null, "RECUSADA".equals(status) ? data : null, data);
        registro.setMensagem("Posso ajudar aos sábados");
        registro.setRecado("Recado preservado");
        var programa = new AtomicReference<>(new Programa(programaId, "Leitura", "Apoio", List.of("Educação"),
                List.of("sábado"), "09:00", "12:00", true, responsavelId, data, null));
        when(programas.findById(programaId)).thenAnswer(c -> Optional.of(programa.get()));
        when(programas.findByIdAndResponsavelId(programaId, responsavelId)).thenAnswer(c -> Optional.of(programa.get()));
        when(programas.findAllById(any())).thenAnswer(c -> List.of(programa.get()));
        when(programas.save(any())).thenAnswer(c -> { programa.set(c.getArgument(0)); return programa.get(); });
        when(inscricoes.findByVoluntarioId(participanteId)).thenReturn(List.of(registro));
        when(inscricoes.findByProgramaId(programaId)).thenReturn(List.of(registro));
        when(inscricoes.existsByVoluntarioIdAndProgramaId(participanteId, programaId)).thenReturn(true);
        var programasService = new ProgramaService(programas, usuarios);
        var inscricoesService = new InscricaoService(inscricoes, programas, usuarios);

        for (boolean ativo : new boolean[]{false, true, false, true}) {
            programasService.alterarDisponibilidade(programaId, ativo, responsavelId);
            var minha = inscricoesService.listarMinhasInscricoes(participanteId).getFirst();
            var historico = inscricoesService.consultarHistoricoParticipacao(participanteId).getFirst();
            assertEquals(status, minha.status());
            assertEquals(status, historico.situacao());
            assertEquals(ativo, minha.programaAtivo());
            assertEquals(ativo, historico.programaAtivo());
            assertEquals(data, minha.dataInscricao());
            assertEquals("inscricao-original", minha.inscricaoId());
            assertEquals("Posso ajudar aos sábados", minha.mensagem());
            assertEquals("Recado preservado", minha.recado());
            assertEquals(1, inscricoesService.listarInscritos(programaId, null, responsavelId).size());
            if (ativo) {
                assertNotNull(minha.dataReativacao());
                assertEquals(HttpStatus.CONFLICT, assertThrows(AuthException.class,
                        () -> inscricoesService.inscrever(programaId, participanteId, "Outra mensagem")).getStatus());
            } else {
                assertThrows(IllegalArgumentException.class,
                        () -> inscricoesService.inscrever(programaId, participanteId, null));
            }
            assertEquals(data, registro.getDataAtualizacao());
        }
        verify(inscricoes, never()).save(any());
        verify(inscricoes, never()).saveAll(any());

        var reativacao = programa.get().getDataReativacao();
        programasService.alterarDisponibilidade(programaId, true, responsavelId);
        assertEquals(reativacao, programa.get().getDataReativacao());
        var pedido = new ProgramaRequestDTO("Leitura", "Apoio", List.of("Educação"), List.of("sábado"), "09:00", "12:00", false);
        programasService.editar(programaId, pedido, responsavelId);
        assertEquals(reativacao, programa.get().getDataReativacao());
        programasService.editar(programaId, new ProgramaRequestDTO(pedido.nome(), pedido.descricao(), pedido.categoria(),
                pedido.diasDisponiveis(), pedido.horarioInicio(), pedido.horarioFim(), true), responsavelId);
        assertNotNull(programa.get().getDataReativacao());
        assertEquals(status, registro.getStatus());
    }
}
