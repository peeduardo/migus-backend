package br.com.senac.migus.service;

import br.com.senac.migus.dto.ProgramaRequestDTO;
import br.com.senac.migus.model.Usuarios;
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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgramaMongoIT {
    @Test
    void devePersistirProgramaNoBancoDeTestes() {
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

        // Contexto mínimo: não carrega a aplicação nem a rotina de inicialização.
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
            var service = new ProgramaService(programas, usuarios);
            var responsavel = new Usuarios();
            String usuarioId = new ObjectId().toHexString();
            String nomePrograma = "Programa de teste " + UUID.randomUUID();
            responsavel.setId(usuarioId);
            responsavel.setNome("Responsável fictício");
            responsavel.setEmail(UUID.randomUUID() + "@exemplo.invalid");
            responsavel.setTipoUsuario("RESPONSAVEL");
            responsavel.setAtivo(true);

            try {
                usuarios.save(responsavel);
                var pedido = new ProgramaRequestDTO(nomePrograma, "Descrição fictícia", List.of("Educação"),
                        List.of("segunda"), "09:00", "12:00", true);
                var salvo = service.cadastrar(pedido, usuarioId);
                var encontrado = programas.findById(salvo.getId()).orElseThrow();

                assertEquals(nomePrograma, encontrado.getNome());
                assertEquals("Descrição fictícia", encontrado.getDescricao());
                assertEquals(List.of("Educação"), encontrado.getCategoria());
                assertEquals(List.of("segunda"), encontrado.getDiasDisponiveis());
                assertEquals("09:00", encontrado.getHorarioInicio());
                assertEquals("12:00", encontrado.getHorarioFim());
                assertTrue(encontrado.getAtivo());
                assertEquals(usuarioId, encontrado.getResponsavelId());
                assertNotNull(encontrado.getDataCadastro());
                assertNull(encontrado.getDataAtualizacao());
                var documento = mongo.getCollection("programas").find(eq("_id", new ObjectId(salvo.getId()))).first();
                assertNotNull(documento);
                assertInstanceOf(ObjectId.class, documento.get("_id"));
                assertInstanceOf(ObjectId.class, documento.get("responsavelId"));
                assertInstanceOf(Date.class, documento.get("dataCadastro"));
            } finally {
                // Identificadores exclusivos desta execução; nunca remove coleções.
                try {
                    mongo.getCollection("programas").deleteOne(and(
                            eq("responsavelId", new ObjectId(usuarioId)), eq("nome", nomePrograma)));
                } finally {
                    usuarios.deleteById(usuarioId);
                }
                assertEquals(0L, mongo.getCollection("programas").countDocuments(and(
                        eq("responsavelId", new ObjectId(usuarioId)), eq("nome", nomePrograma))),
                        "O programa criado pelo teste deve ser removido");
                assertFalse(usuarios.existsById(usuarioId), "O usuário criado pelo teste deve ser removido");
            }
        }
    }
}
