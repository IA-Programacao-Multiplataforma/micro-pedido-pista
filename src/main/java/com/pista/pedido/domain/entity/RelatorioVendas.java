package com.pista.pedido.domain.entity;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioVendas(
        Totais totais,
        List<PorEvento> eventos
) {

    public RelatorioVendas {
        eventos = List.copyOf(eventos);
    }

    public record Totais(
            int pedidosRealizados,
            int pedidosCancelados,
            int pedidosEmAndamento,
            int ingressosVendidos,
            BigDecimal receita
    ) {
    }

    public record PorEvento(
            String eventoId,
            String eventoTitulo,
            int pedidosRealizados,
            int ingressosVendidos,
            BigDecimal receita,
            List<PorSetor> setores
    ) {

        public PorEvento {
            setores = List.copyOf(setores);
        }
    }

    public record PorSetor(String setorNome, int ingressosVendidos, BigDecimal receita) {
    }
}
