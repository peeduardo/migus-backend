package br.com.senac.migus.dto;

import br.com.senac.migus.model.Usuarios;

import java.time.LocalDate;
import java.util.Date;

public record ExVoluntarioDTO(
        String id,
        String nome,
        String email,
        String telefone,
        String tipoUsuario,
        Boolean cadastroAtivo,
        boolean saidaRegistrada,
        Date ultimaSaida,
        LocalDate dataCadastro,
        LocalDate dataAtualizacao
) {
    public static ExVoluntarioDTO of(Usuarios usuario, boolean saidaRegistrada, Date ultimaSaida) {
        return new ExVoluntarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getTipoUsuario(),
                usuario.getAtivo(),
                saidaRegistrada,
                ultimaSaida,
                usuario.getDataCadastro(),
                usuario.getDataAtualizacao()
        );
    }
}
