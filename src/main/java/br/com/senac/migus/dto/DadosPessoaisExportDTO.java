package br.com.senac.migus.dto;

import br.com.senac.migus.model.Usuarios;
import java.time.LocalDate;
import java.util.List;

public record DadosPessoaisExportDTO(
        String id,
        String nome,
        String email,
        String cpf,
        String telefone,
        LocalDate dataNascimento,
        String tipoUsuario,
        Boolean ativo,
        LocalDate dataCadastro,
        LocalDate dataAtualizacao,
        List<MinhaInscricaoDTO> inscricoes
) {
    public static DadosPessoaisExportDTO of(Usuarios usuario, List<MinhaInscricaoDTO> inscricoes) {
        return new DadosPessoaisExportDTO(
                usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getCpf(), usuario.getTelefone(), usuario.getDataNascimento(),
                usuario.getTipoUsuario(), usuario.getAtivo(),
                usuario.getDataCadastro(), usuario.getDataAtualizacao(),
                inscricoes);
    }
}
