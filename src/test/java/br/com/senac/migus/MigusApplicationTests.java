package br.com.senac.migus;

import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigusApplicationTests {

	@Mock
	UsuariosRepository repository;

	@Test
	void deveSalvarUsuarioQuandoEmailNaoExiste() throws Exception {
		// Preparação
		var application = new MigusApplication();
		var usuarioSalvo = new Usuarios();
		usuarioSalvo.setNome("João da Silva");
		usuarioSalvo.setEmail("joao@email.com");
		when(repository.existsByEmail("joao@email.com")).thenReturn(false);
		when(repository.save(any(Usuarios.class))).thenReturn(usuarioSalvo);
		when(repository.count()).thenReturn(1L);

		// Execução
		application.testarMongoDB(repository).run();

		// Verificação
		var usuario = ArgumentCaptor.forClass(Usuarios.class);
		verify(repository).existsByEmail("joao@email.com");
		verify(repository).save(usuario.capture());
		assertEquals("João da Silva", usuario.getValue().getNome());
		assertEquals("joao@email.com", usuario.getValue().getEmail());
		verify(repository).count();
	}

	@Test
	void naoDeveSalvarUsuarioQuandoEmailJaExiste() throws Exception {
		// Preparação
		var application = new MigusApplication();
		when(repository.existsByEmail("joao@email.com")).thenReturn(true);
		when(repository.count()).thenReturn(1L);

		// Execução
		application.testarMongoDB(repository).run();

		// Verificação
		verify(repository).existsByEmail("joao@email.com");
		verify(repository, never()).save(any(Usuarios.class));
		verify(repository).count();
	}

}
