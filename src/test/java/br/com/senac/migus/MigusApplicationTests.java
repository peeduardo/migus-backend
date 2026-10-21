
package br.com.senac.migus;

import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.CommandLineRunner;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigusApplicationTest {

	@Mock
	private UsuariosRepository repository;

	private MigusApplication application;

	@BeforeEach
	void setUp() {
		application = new MigusApplication();
	}

	private Usuarios criarUsuario() {
		return new Usuarios(
				"João da Silva",
				"joao@email.com",
				"$2a$10$hashDeExemplo",
				"12345678900",
				"11999999999",
				LocalDate.of(2000, 5, 10),
				"VOLUNTARIO",
				true,
				LocalDate.of(2026, 10, 18),
				null
		);
	}

	@Test
	void deveSalvarUsuarioQuandoEmailNaoExiste() throws Exception {
		Usuarios usuario = criarUsuario();

		when(repository.existsByEmail("joao@email.com"))
				.thenReturn(false);

		when(repository.save(any(Usuarios.class)))
				.thenReturn(usuario);

		when(repository.count()).thenReturn(1L);

		CommandLineRunner runner =
				application.testarMongoDB(repository);

		runner.run(new String[]{});

		verify(repository).existsByEmail("joao@email.com");
		verify(repository).save(any(Usuarios.class));
		verify(repository).count();
	}

	@Test
	void naoDeveSalvarUsuarioQuandoEmailJaExiste() throws Exception {
		Usuarios usuario = criarUsuario();

		when(repository.existsByEmail(usuario.getEmail()))
				.thenReturn(true);

		when(repository.count()).thenReturn(1L);

		CommandLineRunner runner =
				application.testarMongoDB(repository);

		runner.run(new String[]{});

		verify(repository).existsByEmail("joao@email.com");
		verify(repository, never()).save(any(Usuarios.class));
		verify(repository).count();
	}

	@Test
	void deveCriarUsuarioComDadosCorretos() {
		Usuarios usuario = criarUsuario();

		assertEquals("João da Silva", usuario.getNome());
		assertEquals("joao@email.com", usuario.getEmail());
		assertEquals("$2a$10$hashDeExemplo", usuario.getSenhaHash());
		assertEquals("12345678900", usuario.getCpf());
		assertEquals("11999999999", usuario.getTelefone());
		assertEquals(
				LocalDate.of(2000, 5, 10),
				usuario.getDataNascimento()
		);
		assertEquals("VOLUNTARIO", usuario.getTipoUsuario());
		assertTrue(usuario.getAtivo());
		assertEquals(
				LocalDate.of(2026, 10, 18),
				usuario.getDataCadastro()
		);
		assertNull(usuario.getDataAtualizacao());
	}

	@Test
	void deveAtualizarDadosDoUsuario() {
		Usuarios usuario = criarUsuario();

		usuario.setNome("Maria da Silva");
		usuario.setEmail("maria@email.com");
		usuario.setSenhaHash("novoHash");
		usuario.setCpf("98765432100");
		usuario.setTelefone("11988887777");
		usuario.setDataNascimento(LocalDate.of(1999, 8, 20));
		usuario.setTipoUsuario("GESTOR");
		usuario.setAtivo(false);
		usuario.setDataCadastro(LocalDate.of(2026, 10, 1));
		usuario.setDataAtualizacao(LocalDate.of(2026, 10, 18));

		assertEquals("Maria da Silva", usuario.getNome());
		assertEquals("maria@email.com", usuario.getEmail());
		assertEquals("novoHash", usuario.getSenhaHash());
		assertEquals("98765432100", usuario.getCpf());
		assertEquals("11988887777", usuario.getTelefone());
		assertEquals(
				LocalDate.of(1999, 8, 20),
				usuario.getDataNascimento()
		);
		assertEquals("GESTOR", usuario.getTipoUsuario());
		assertFalse(usuario.getAtivo());
		assertEquals(
				LocalDate.of(2026, 10, 1),
				usuario.getDataCadastro()
		);
		assertEquals(
				LocalDate.of(2026, 10, 18),
				usuario.getDataAtualizacao()
		);
	}
}
