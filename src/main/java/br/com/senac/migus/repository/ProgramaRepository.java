package br.com.senac.migus.repository;

import br.com.senac.migus.model.Programa;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProgramaRepository extends MongoRepository<Programa, String> {
}
