package com.pista.pedido.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.evento.PedidoCancelado;
import com.pista.pedido.domain.evento.PedidoCriado;
import com.pista.pedido.domain.evento.PedidoRealizado;
import com.pista.pedido.domain.exception.RegraDeNegocioException;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;

class PedidoServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final IngressoRepository ingressos = mock(IngressoRepository.class);
    private final PublicadorDeEventos publicador = mock(PublicadorDeEventos.class);
    private final UnidadeDeTrabalho transacao = new UnidadeDeTrabalho() {
        @Override
        public <T> T executar(Supplier<T> trabalho) {
            return trabalho.get();
        }
    };
    private final PedidoService service = new PedidoService(pedidos, ingressos, publicador, transacao,
            Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void criarGravaProcessandoEEnviaPedidoCriadoAoEstoque() {
        when(pedidos.buscarPorUsuarioEChave("ana", "k1")).thenReturn(Optional.empty());

        Pedido pedido = service.criar("ana", "k1", "s1", List.of(new PedidoCriado.Item("pista", 2)));

        assertThat(pedido.status()).isEqualTo(StatusPedido.PROCESSANDO);
        verify(pedidos).inserir(pedido);
        ArgumentCaptor<Object> mensagem = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq(PedidoCriado.ROUTING_KEY), mensagem.capture());
        assertThat(((PedidoCriado) mensagem.getValue()).pedidoId()).isEqualTo(pedido.id());
    }

    @Test
    void mesmaChaveDevolveOMesmoPedidoSemEnviarDeNovo() {
        Pedido existente = processando();
        when(pedidos.buscarPorUsuarioEChave("ana", "k1")).thenReturn(Optional.of(existente));

        assertThat(service.criar("ana", "k1", "s1", List.of(new PedidoCriado.Item("pista", 1)))).isEqualTo(existente);
        verify(publicador, never()).publicar(any(), any());
    }

    @Test
    void pedidoSemItensNaoECriado() {
        assertThatThrownBy(() -> service.criar("ana", "k1", "s1", List.of()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aoRealizadoMudaParaRealizadoEEmiteOsIngressos() {
        when(pedidos.buscarPorId("p1")).thenReturn(Optional.of(processando()));
        when(pedidos.atualizarSeProcessando(any())).thenReturn(true);

        service.aoRealizado(new PedidoRealizado("p1", "e1", "Show", LocalDateTime.of(2026, 11, 1, 20, 0),
                List.of(new PedidoRealizado.Item("pista", "Pista", 2, new BigDecimal("100.00")))));

        ArgumentCaptor<Pedido> atualizado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).atualizarSeProcessando(atualizado.capture());
        assertThat(atualizado.getValue().status()).isEqualTo(StatusPedido.REALIZADO);
        assertThat(atualizado.getValue().total()).isEqualByComparingTo("200.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<IngressoEmitido>> emitidos = ArgumentCaptor.forClass(List.class);
        verify(ingressos).salvarTodos(emitidos.capture());
        assertThat(emitidos.getValue()).hasSize(2);
    }

    @Test
    void aoCanceladoMudaParaCanceladoComOMotivo() {
        when(pedidos.buscarPorId("p1")).thenReturn(Optional.of(processando()));

        service.aoCancelado(new PedidoCancelado("p1", "Ingressos insuficientes no setor Pista"));

        ArgumentCaptor<Pedido> atualizado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).atualizarSeProcessando(atualizado.capture());
        assertThat(atualizado.getValue().status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(atualizado.getValue().motivo()).contains("insuficientes");
        verify(ingressos, never()).salvarTodos(anyList());
    }

    private static Pedido processando() {
        Instant agora = Instant.parse("2026-10-10T12:00:00Z");
        return new Pedido("p1", "ana", "k1", "s1", null, null, null,
                List.of(new ItemPedido("pista", null, 2, null, null)), null, StatusPedido.PROCESSANDO, null, agora, agora);
    }
}
