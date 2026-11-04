package br.com.senac.migus.repository;

import br.com.senac.migus.model.Inscricao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InscricaoRepository extends MongoRepository<Inscricao, String> {
    List<Inscricao> findByProgramaId(String programaId);

    boolean existsByVoluntarioIdAndProgramaId(String voluntarioId, String programaId);
}
