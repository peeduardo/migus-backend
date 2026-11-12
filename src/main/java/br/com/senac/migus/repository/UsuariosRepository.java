package br.com.senac.migus.repository;
import br.com.senac.migus.model.Usuarios;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.time.LocalDate;
import java.util.Optional;

public interface UsuariosRepository extends MongoRepository<Usuarios, String> {

    @Query("{ '_id': ?0, 'ativo': true, 'tipoUsuario': ?1 }")
    @Update("{ '$set': { 'ativo': false, 'dataAtualizacao': ?2 } }")
    long desativarParticipante(String id, String perfil, LocalDate hoje);

    @Query("{ '_id': ?0, 'ativo': true }")
    @Update("{ '$set': { 'nome': ?1, 'email': ?2, 'telefone': ?3, 'dataNascimento': ?4, 'dataAtualizacao': ?5 } }")
    long atualizarDadosPessoais(String id, String nome, String email, String telefone,
                                LocalDate nascimento, LocalDate hoje);

    boolean existsByIdAndAtivoTrue(String id);

    Optional<Usuarios> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
