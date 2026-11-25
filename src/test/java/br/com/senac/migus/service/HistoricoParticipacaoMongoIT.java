package br.com.senac.migus.service;

import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoParticipacaoMongoIT {
    @Test
    void deveRetornarNoHistoricoOsRegistrosPersistidosDoVoluntario() {
        String uri = System.getenv("MONGODB_TEST_URI");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Configure MONGODB_TEST_URI para o banco exclusivo migus_testes");
        }
        ConnectionString conexao;
        try {
            conexao = new ConnectionString(uri);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("MONGODB_TEST_URI tem formato inválido");
        }
        if (!"migus_testes".equals(conexao.getDatabase())) {
            throw new IllegalStateException("MONGODB_TEST_URI deve selecionar exclusivamente migus_testes");
        }

        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("test");
            context.registerBean(MongoClient.class, () -> MongoClients.create(conexao));
            context.registerBean(MongoTemplate.class,
                    () -> new MongoTemplate(context.getBean(MongoClient.class), "migus_testes"));
            context.refresh();

            var mongo = context.getBean(MongoTemplate.class);
            var factory = new MongoRepositoryFactory(mongo);
            var usuarios = factory.getRepository(UsuariosRepository.class);
            var programas = factory.getRepository(ProgramaRepository.class);
            var inscricoes = factory.getRepository(InscricaoRepository.class);
            var service = new InscricaoService(inscricoes, programas, usuarios);

            String voluntarioId = new ObjectId().toHexString();
            String outroVoluntarioId = new ObjectId().toHexString();
            String programaAntigoId = new ObjectId().toHexString();
            String programaRecenteId = new ObjectId().toHexString();
            String programaAlheioId = new ObjectId().toHexString();
            String inscricaoAntigaId = new ObjectId().toHexString();
            String inscricaoRecenteId = new ObjectId().toHexString();
            String inscricaoAlheiaId = new ObjectId().toHexString();
            String sufixo = UUID.randomUUID().toString();

            var voluntario = usuario(voluntarioId, "voluntario-" + sufixo + "@exemplo.invalid");
            var outroVoluntario = usuario(outroVoluntarioId, "outro-" + sufixo + "@exemplo.invalid");
            var dataInscricaoAntiga = data("2026-10-01T12:00:00Z");
            var dataInicioAntiga = data("2026-10-02T12:00:00Z");
            var antiga = new Inscricao(inscricaoAntigaId, voluntarioId, programaAntigoId, "APROVADA",
                    dataInscricaoAntiga, dataInicioAntiga, null, dataInicioAntiga);
            var dataInscricaoRecente = data("2026-11-01T12:00:00Z");
            var dataInicioRecente = data("2026-11-02T12:00:00Z");
            var recente = new Inscricao(inscricaoRecenteId, voluntarioId, programaRecenteId, "APROVADA",
                    dataInscricaoRecente, dataInicioRecente, null, dataInicioRecente);
            var dataFimRecente = data("2026-11-10T12:00:00Z");
            recente.sair(dataFimRecente);
            var alheia = new Inscricao(inscricaoAlheiaId, outroVoluntarioId, programaAlheioId, "PENDENTE",
                    data("2026-11-12T12:00:00Z"), null, null, null);
            var programaAntigo = programa(programaAntigoId, "Programa antigo");
            var programaRecente = programa(programaRecenteId, "Programa recente");
            var programaAlheio = programa(programaAlheioId, "Programa de outra pessoa");

            try {
                usuarios.saveAll(List.of(voluntario, outroVoluntario));
                programas.saveAll(List.of(programaAntigo, programaRecente, programaAlheio));
                inscricoes.saveAll(List.of(antiga, recente, alheia));

                var historico = service.consultarHistoricoParticipacao(voluntarioId);
                var registrosPersistidos = inscricoes.findByVoluntarioId(voluntarioId);

                assertEquals(2, registrosPersistidos.size());
                assertEquals(2, historico.size());
                assertEquals(inscricaoRecenteId, historico.get(0).inscricaoId());
                assertEquals(programaRecenteId, historico.get(0).programaId());
                assertEquals("Programa recente", historico.get(0).programaNome());
                assertEquals("SAIDA", historico.get(0).situacao());
                assertEquals(dataInscricaoRecente, historico.get(0).dataInscricao());
                assertEquals(dataInicioRecente, historico.get(0).inicioParticipacao());
                assertEquals(dataFimRecente, historico.get(0).fimParticipacao());
                assertEquals(inscricaoAntigaId, historico.get(1).inscricaoId());
                assertEquals("Programa antigo", historico.get(1).programaNome());
                assertEquals("APROVADA", historico.get(1).situacao());
                assertTrue(registrosPersistidos.stream().anyMatch(i -> i.getId().equals(historico.get(0).inscricaoId())));
                assertFalse(historico.stream().anyMatch(i -> i.inscricaoId().equals(inscricaoAlheiaId)));
            } finally {
                inscricoes.deleteAllById(List.of(inscricaoAntigaId, inscricaoRecenteId, inscricaoAlheiaId));
                programas.deleteAllById(List.of(programaAntigoId, programaRecenteId, programaAlheioId));
                usuarios.deleteAllById(List.of(voluntarioId, outroVoluntarioId));
                assertTrue(inscricoes.findByVoluntarioId(voluntarioId).isEmpty());
                assertTrue(inscricoes.findByVoluntarioId(outroVoluntarioId).isEmpty());
                assertFalse(usuarios.existsById(voluntarioId));
                assertFalse(usuarios.existsById(outroVoluntarioId));
            }
        }
    }

    private static Usuarios usuario(String id, String email) {
        var usuario = new Usuarios();
        usuario.setId(id);
        usuario.setNome("Voluntário de teste");
        usuario.setEmail(email);
        usuario.setTipoUsuario("VOLUNTARIO");
        usuario.setAtivo(true);
        return usuario;
    }

    private static Programa programa(String id, String nome) {
        return new Programa(id, nome, "Descrição de teste", List.of("Teste"), List.of("segunda"),
                "09:00", "12:00", true, new ObjectId().toHexString(), new Date(), null);
    }

    private static Date data(String valor) {
        return Date.from(Instant.parse(valor));
    }
}
