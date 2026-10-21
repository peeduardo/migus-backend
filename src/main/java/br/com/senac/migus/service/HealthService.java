package br.com.senac.migus.service;

import br.com.senac.migus.dto.HealthResponseDTO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponseDTO check() {
        return new HealthResponseDTO("UP", LocalDateTime.now());
    }
}
