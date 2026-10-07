package com.pista.pedido.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.RelatorioVendas;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.repository.PedidoRepository;

class RelatorioServiceTest {

    private final PedidoRepository repositorio = mock(PedidoRepository.class);
    private final RelatorioService service = new RelatorioService(repositorio);

    private static ItemPedido item(String setor, int qtd, String preco) {
        BigDecimal unitario = new BigDecimal(preco);
        return new ItemPedido("s-" + setor, setor, qtd, unitario, unitario.multiply(BigDecimal.valueOf(qtd)));
    }

    private static Pedido pedido(String id, String eventoId, String titulo, StatusPedido status, ItemPedido... itens) {
        BigDecimal total = List.of(itens).stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(id, "u-1", "chave-" + id, "sess-1", eventoId, titulo,
                LocalDateTime.of(2027, 10, 18, 20, 0), "res-" + id, List.of(itens), total, status, null,
                Instant.parse("2026-10-01T12:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"));
    }

    @Test
    void semPedidosORelatorioVemZerado() {
        when(repositorio.listarTodos()).thenReturn(List.of());

        RelatorioVendas relatorio = service.gerar();

        assertThat(relatorio.eventos()).isEmpty();
        assertThat(relatorio.totais().receita()).isEqualByComparingTo("0");
        assertThat(relatorio.totais().ingressosVendidos()).isZero();
    }

    @Test
    void soPedidosPagosContamComoVenda() {
        when(repositorio.listarTodos()).thenReturn(List.of(
                pedido("1", "e1", "Festival", StatusPedido.PAGO, item("Pista", 2, "100.00")),
                pedido("2", "e1", "Festival", StatusPedido.RECUSADO, item("Pista", 4, "100.00")),
                pedido("3", "e1", "Festival", StatusPedido.CANCELADO, item("Pista", 1, "100.00")),
                pedido("4", "e1", "Festival", StatusPedido.PENDENTE, item("Pista", 1, "100.00")),
                pedido("5", "e1", "Festival", StatusPedido.CONFIRMANDO, item("Pista", 1, "100.00"))));

        RelatorioVendas.Totais totais = service.gerar().totais();

        assertThat(totais.pedidosPagos()).isEqualTo(1);
        assertThat(totais.pedidosRecusados()).isEqualTo(1);
        assertThat(totais.pedidosCancelados()).isEqualTo(1);
        assertThat(totais.pedidosEmAndamento()).isEqualTo(2);
        assertThat(totais.ingressosVendidos()).isEqualTo(2);
        assertThat(totais.receita()).isEqualByComparingTo("200.00");
    }

    @Test
    void somaPorEventoESetorEOrdenaPelaMaiorReceita() {
        when(repositorio.listarTodos()).thenReturn(List.of(
                pedido("1", "e1", "Festival", StatusPedido.PAGO, item("Pista", 2, "100.00"), item("Camarote", 1, "300.00")),
                pedido("2", "e1", "Festival", StatusPedido.PAGO, item("Pista", 3, "100.00")),
                pedido("3", "e2", "Teatro", StatusPedido.PAGO, item("Plateia", 1, "50.00"))));

        RelatorioVendas relatorio = service.gerar();

        assertThat(relatorio.eventos()).extracting(RelatorioVendas.PorEvento::eventoTitulo)
                .containsExactly("Festival", "Teatro");

        RelatorioVendas.PorEvento festival = relatorio.eventos().get(0);
        assertThat(festival.pedidosPagos()).isEqualTo(2);
        assertThat(festival.ingressosVendidos()).isEqualTo(6);
        assertThat(festival.receita()).isEqualByComparingTo("800.00");
        assertThat(festival.setores()).extracting(RelatorioVendas.PorSetor::setorNome)
                .containsExactly("Pista", "Camarote");
        assertThat(festival.setores().get(0).ingressosVendidos()).isEqualTo(5);
        assertThat(festival.setores().get(0).receita()).isEqualByComparingTo("500.00");
        assertThat(festival.setores().get(1).receita()).isEqualByComparingTo("300.00");

        assertThat(relatorio.totais().receita()).isEqualByComparingTo("850.00");
        assertThat(relatorio.totais().ingressosVendidos()).isEqualTo(7);
    }

    @Test
    void eventoSemVendaPagaNaoApareceNoRelatorio() {
        when(repositorio.listarTodos()).thenReturn(List.of(
                pedido("1", "e1", "Festival", StatusPedido.RECUSADO, item("Pista", 2, "100.00"))));

        assertThat(service.gerar().eventos()).isEmpty();
    }
}
