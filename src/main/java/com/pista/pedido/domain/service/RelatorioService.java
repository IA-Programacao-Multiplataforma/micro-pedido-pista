package com.pista.pedido.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.RelatorioVendas;
import com.pista.pedido.domain.entity.RelatorioVendas.PorEvento;
import com.pista.pedido.domain.entity.RelatorioVendas.PorSetor;
import com.pista.pedido.domain.entity.RelatorioVendas.Totais;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.repository.PedidoRepository;

public class RelatorioService {

    private final PedidoRepository pedidos;

    public RelatorioService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public RelatorioVendas gerar() {
        List<Pedido> todos = pedidos.listarTodos();

        Map<String, EventoAcumulado> porEvento = new LinkedHashMap<>();
        int pagos = 0, recusados = 0, cancelados = 0, emAndamento = 0, ingressos = 0;
        BigDecimal receita = BigDecimal.ZERO;

        for (Pedido pedido : todos) {
            switch (pedido.status()) {
                case RECUSADO -> recusados++;
                case CANCELADO -> cancelados++;
                case PENDENTE, CONFIRMANDO -> emAndamento++;
                case PAGO -> {
                    pagos++;
                    receita = receita.add(pedido.total());
                    EventoAcumulado evento = porEvento.computeIfAbsent(pedido.eventoId(),
                            id -> new EventoAcumulado(id, pedido.eventoTitulo()));
                    evento.pedidosPagos++;
                    evento.receita = evento.receita.add(pedido.total());
                    for (ItemPedido item : pedido.itens()) {
                        ingressos += item.quantidade();
                        evento.ingressos += item.quantidade();
                        SetorAcumulado setor = evento.setores.computeIfAbsent(item.setorNome(), n -> new SetorAcumulado());
                        setor.ingressos += item.quantidade();
                        setor.receita = setor.receita.add(item.subtotal());
                    }
                }
            }
        }

        List<PorEvento> eventos = porEvento.values().stream()
                .map(EventoAcumulado::toPorEvento)
                .sorted(Comparator.comparing(PorEvento::receita).reversed())
                .toList();

        return new RelatorioVendas(new Totais(pagos, recusados, cancelados, emAndamento, ingressos, receita), eventos);
    }

    private static class EventoAcumulado {
        final String eventoId;
        final String titulo;
        final Map<String, SetorAcumulado> setores = new LinkedHashMap<>();
        int pedidosPagos;
        int ingressos;
        BigDecimal receita = BigDecimal.ZERO;

        EventoAcumulado(String eventoId, String titulo) {
            this.eventoId = eventoId;
            this.titulo = titulo;
        }

        PorEvento toPorEvento() {
            List<PorSetor> lista = setores.entrySet().stream()
                    .map(e -> new PorSetor(e.getKey(), e.getValue().ingressos, e.getValue().receita))
                    .toList();
            return new PorEvento(eventoId, titulo, pedidosPagos, ingressos, receita, lista);
        }
    }

    private static class SetorAcumulado {
        int ingressos;
        BigDecimal receita = BigDecimal.ZERO;
    }
}
