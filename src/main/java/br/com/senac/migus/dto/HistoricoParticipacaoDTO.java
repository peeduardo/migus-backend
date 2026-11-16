package br.com.senac.migus.dto;

import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;

import java.util.Date;

public record HistoricoParticipacaoDTO(
        String inscricaoId,
        String programaId,
        String programaNome,
        Date dataInscricao,
        Date inicioParticipacao,
        Date fimParticipacao,
        String situacao
) {
    public static HistoricoParticipacaoDTO of(Inscricao inscricao, Programa programa) {
        String situacao = inscricao.getStatus();
        Date inicio = switch (situacao) {
            case "APROVADA", "SAIDA", "TRANSFERIDA" -> inscricao.getDataAprovacao();
            default -> null;
        };
        Date fim = switch (situacao) {
            case "SAIDA" -> inscricao.getDataSaida();
            case "TRANSFERIDA" -> inscricao.getDataAtualizacao();
            default -> null;
        };

        return new HistoricoParticipacaoDTO(
                inscricao.getId(), inscricao.getProgramaId(),
                programa == null ? null : programa.getNome(), inscricao.getDataInscricao(),
                inicio, fim, situacao);
    }
}
