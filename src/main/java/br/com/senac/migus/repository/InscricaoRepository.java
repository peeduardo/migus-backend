package br.com.senac.migus.repository;

import br.com.senac.migus.model.Inscricao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InscricaoRepository extends MongoRepository<Inscricao, String> {
    List<Inscricao> findByVoluntarioId(String voluntarioId);

    List<Inscricao> findByProgramaId(String programaId);

    List<Inscricao> findByProgramaIdAndStatus(String programaId, String status);

    boolean existsByVoluntarioIdAndProgramaId(String voluntarioId, String programaId);
}
