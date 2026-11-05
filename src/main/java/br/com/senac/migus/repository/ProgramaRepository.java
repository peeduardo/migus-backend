package br.com.senac.migus.repository;
import java.util.Optional;
import br.com.senac.migus.model.Programa;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProgramaRepository extends MongoRepository<Programa, String> {
    List<Programa> findByAtivoTrue();
    List<Programa> findByResponsavelId(String responsavelId);
    Optional<Programa> findByIdAndResponsavelId(String id, String responsavelId);
}
