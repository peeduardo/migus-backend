package br.com.senac.migus;

import br.com.senac.migus.model.Usuarios;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuariosTests {

	@Test
	void deveGuardarOsDadosDoUsuario() {
		// Preparação
		var usuario = new Usuarios();
		var nascimento = LocalDate.of(2000, 5, 10);
		var cadastro = LocalDate.of(2026, 10, 1);
		var atualizacao = LocalDate.of(2026, 10, 2);

		// Execução
		usuario.setId("user-1");
		usuario.setNome("Fulano");
		usuario.setEmail("fulano@email.com");
		usuario.setSenhaHash("hash-ficticio");
		usuario.setCpf("00000000000");
		usuario.setTelefone("11900000000");
		usuario.setDataNascimento(nascimento);
		usuario.setTipoUsuario("cliente");
		usuario.setAtivo(true);
		usuario.setDataCadastro(cadastro);
		usuario.setDataAtualizacao(atualizacao);

		// Verificação
		assertEquals("user-1", usuario.getId());
		assertEquals("Fulano", usuario.getNome());
		assertEquals("fulano@email.com", usuario.getEmail());
		assertEquals("hash-ficticio", usuario.getSenhaHash());
		assertEquals("00000000000", usuario.getCpf());
		assertEquals("11900000000", usuario.getTelefone());
		assertEquals(nascimento, usuario.getDataNascimento());
		assertEquals("cliente", usuario.getTipoUsuario());
		assertTrue(usuario.getAtivo());
		assertEquals(cadastro, usuario.getDataCadastro());
		assertEquals(atualizacao, usuario.getDataAtualizacao());
	}

}
