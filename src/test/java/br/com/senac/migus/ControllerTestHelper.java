package br.com.senac.migus;

import br.com.senac.migus.model.Programa;

import java.util.Date;

public class ControllerTestHelper {
    public static Programa programaAtivo(String id, String responsavelId) {
        return new Programa(id, "Programa Teste", "Descrição", null, null,
                "09:00", "18:00", true, responsavelId, new Date(), null);
    }
}
