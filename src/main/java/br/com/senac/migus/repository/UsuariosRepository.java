package br.com.senac.migus.repository;
import br.com.senac.migus.model.Usuarios;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuariosRepository extends MongoRepository<Usuarios, String> {

    Optional<Usuarios> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
