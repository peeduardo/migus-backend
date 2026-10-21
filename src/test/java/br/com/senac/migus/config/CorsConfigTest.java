package br.com.senac.migus.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigTest {

    @Test
    void deveRegistrarMapeamentoDeCors() {
        assertThatCode(() -> new CorsConfig().addCorsMappings(new CorsRegistry()))
                .doesNotThrowAnyException();
    }
}
