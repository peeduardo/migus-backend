
package br.com.senac.migus;


import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.UsuariosRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class MigusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MigusApplication.class, args);
	}

	@Bean
	CommandLineRunner testarMongoDB(UsuariosRepository repository) {
		return args -> {

			Usuarios usuario = new Usuarios(
					"João da Silva",
					"joao@email.com",
					"$2a$10$hashDeExemplo",
					"12345678900",
					"11999999999",
					LocalDate.of(2000, 5, 10),
					"VOLUNTARIO",
					true,
					LocalDate.now(),
					null
			);

			if (!repository.existsByEmail(usuario.getEmail())) {
				Usuarios salvo = repository.save(usuario);

				System.out.println("================================");
				System.out.println("USUÁRIO SALVO COM SUCESSO!");
				System.out.println("ID: " + salvo.getId());
				System.out.println("Nome: " + salvo.getNome());
				System.out.println("E-mail: " + salvo.getEmail());
				System.out.println("================================");
			} else {
				System.out.println("Usuário já cadastrado!");
			}

			System.out.println(
					"Total de usuários: " + repository.count()
			);
		};
	}
}
