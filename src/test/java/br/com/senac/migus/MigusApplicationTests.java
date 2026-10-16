package br.com.senac.migus;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class MigusApplicationTests {

	@DynamicPropertySource
	static void configurarMongoDB(DynamicPropertyRegistry registry) {
		String uri = System.getenv("MONGODB_TEST_URI");
		if (uri == null || uri.isBlank()) {
			throw new IllegalStateException(
					"Configure MONGODB_TEST_URI com a conexão exclusiva de testes antes de executar os testes.");
		}
		registry.add("spring.mongodb.uri", () -> uri);
		registry.add("spring.mongodb.database", () -> "migus_testes");
	}

	@Autowired
	MongoClient mongoClient;

	@Test
	void contextLoads() {
	}

	@Test
	void deveInserirEBuscarDocumentoNoMongoDB() {
		// Preparação
		var colecao = mongoClient.getDatabase("migus_testes").getCollection("c05_documentos");
		var id = new ObjectId();
		var documento = new Document("_id", id)
				.append("nome", "Estudante de teste")
				.append("idade", 21);

		try {
			// Execução
			colecao.insertOne(documento);
			var encontrado = colecao.find(eq("_id", id)).first();

			// Verificação
			assertNotNull(encontrado);
			assertEquals(id, encontrado.getObjectId("_id"));
			assertEquals("Estudante de teste", encontrado.getString("nome"));
			assertEquals(Integer.valueOf(21), encontrado.getInteger("idade"));
		} finally {
			// Remove documento desta execução, mesmo se uma verificação falhar.
			colecao.deleteOne(eq("_id", id));
		}
	}

}
