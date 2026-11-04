package br.com.senac.migus.repository;

import br.com.senac.migus.model.Programa;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProgramaRepository extends MongoRepository<Programa, String> {
    List<Programa> findByAtivoTrue();
}
