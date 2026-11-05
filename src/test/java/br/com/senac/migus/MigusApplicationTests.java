package br.com.senac.migus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MigusApplicationTests {

	@Test
	void deveSerUmaAplicacaoSpringBoot() {
		assertNotNull(MigusApplication.class.getAnnotation(SpringBootApplication.class));
	}

}
