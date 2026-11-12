package br.com.senac.migus.dto;

import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import java.util.Date;

public record MinhaInscricaoDTO(
        String inscricaoId,
        String programaId,
        String programaNome,
        Date dataInscricao,
        String status
) {
    public static MinhaInscricaoDTO of(Inscricao inscricao, Programa programa) {
        return new MinhaInscricaoDTO(inscricao.getId(), inscricao.getProgramaId(),
                programa == null ? null : programa.getNome(),
                inscricao.getDataInscricao(), inscricao.getStatus());
    }
}
