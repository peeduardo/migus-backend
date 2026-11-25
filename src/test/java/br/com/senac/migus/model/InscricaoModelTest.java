package br.com.senac.migus.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class InscricaoModelTest {

    @Test
    void cancelar_deveDefinirStatusCanceladaEAtualizarData() {
        var inscricao = new Inscricao("id", "vol", "prog", "APROVADA",
                new Date(), new Date(), null, null);
        Date agora = new Date();

        inscricao.cancelar(agora);

        assertThat(inscricao.getStatus()).isEqualTo("CANCELADA");
        assertThat(inscricao.getDataAtualizacao()).isEqualTo(agora);
    }

    @Test
    void suspender_deveDefinirStatusSuspensaEAtualizarData() {
        var inscricao = new Inscricao("id", "vol", "prog", "APROVADA",
                new Date(), new Date(), null, null);
        Date agora = new Date();

        inscricao.suspender(agora);

        assertThat(inscricao.getStatus()).isEqualTo("SUSPENSA");
        assertThat(inscricao.getDataAtualizacao()).isEqualTo(agora);
    }
}
