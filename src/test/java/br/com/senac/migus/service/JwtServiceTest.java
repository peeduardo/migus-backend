package br.com.senac.migus.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService() {
        String chave = Base64.getEncoder().encodeToString(
                "chave-ficticia-apenas-para-testes-123456789".getBytes());
        return new JwtService(chave, 3_600_000L);
    }

    @Test
    void getExpirationMs_deveRetornarValorConfigurado() {
        assertThat(jwtService().getExpirationMs()).isEqualTo(3_600_000L);
    }

    @Test
    void generateToken_deveGerarTokenQuePermiteExtrairUserId() {
        var service = jwtService();
        String token = service.generateToken("user-1", "ana@exemplo.com", "VOLUNTARIO");
        assertThat(service.extractUserId(token)).isEqualTo("user-1");
    }
}
