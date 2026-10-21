package br.com.senac.migus.exception;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.migus.dto.ErrorResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveRetornar404QuandoRecursoNaoEncontrado() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleNotFound(new ResourceNotFoundException("Recurso não encontrado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Recurso não encontrado");
    }

    @Test
    void deveRetornar500ParaErroGenerico() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleGeneric(new RuntimeException("falha inesperada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Erro interno no servidor");
    }
}
