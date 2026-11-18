package com.pista.pedido.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.PedidoComIngressos;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.evento.PedidoCancelado;
import com.pista.pedido.domain.evento.PedidoCriado;
import com.pista.pedido.domain.evento.PedidoRealizado;
import com.pista.pedido.domain.exception.DuplicadoException;
import com.pista.pedido.domain.exception.NaoEncontradoException;
import com.pista.pedido.domain.exception.RegraDeNegocioException;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;

/*
 * Ciclo do pedido:
 *   1. criar(): grava o pedido como PROCESSANDO e envia "pedido.criado" ao Estoque (RabbitMQ);
 *   2. o Estoque responde "pedido.realizado" -> aoRealizado(): status REALIZADO e emite os ingressos;
 *      ou responde "pedido.cancelado"       -> aoCancelado(): status CANCELADO.
 */
public class PedidoService {

    private final PedidoRepository pedidos;
    private final IngressoRepository ingressos;
    private final PublicadorDeEventos publicador;
    private final UnidadeDeTrabalho transacao;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidos, IngressoRepository ingressos, PublicadorDeEventos publicador,
                         UnidadeDeTrabalho transacao, Clock clock) {
        this.pedidos = pedidos;
        this.ingressos = ingressos;
        this.publicador = publicador;
        this.transacao = transacao;
        this.clock = clock;
    }

    public Pedido criar(String usuarioId, String chaveIdempotencia, String sessaoId, List<PedidoCriado.Item> itens) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
            throw new RegraDeNegocioException("Idempotency-Key é obrigatório");
        }
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(i -> i.quantidade() <= 0)) {
            throw new RegraDeNegocioException("Escolha pelo menos um ingresso");
        }

        // Mesma Idempotency-Key = mesmo clique (duplo toque, rede instável): devolve o pedido que já existe
        var repetido = pedidos.buscarPorUsuarioEChave(usuarioId, chaveIdempotencia);
        if (repetido.isPresent()) {
            return repetido.get();
        }

        Instant agora = Instant.now(clock);
        List<ItemPedido> solicitados = itens.stream()
                .map(i -> new ItemPedido(i.setorId(), null, i.quantidade(), null, null))
                .toList();
        Pedido novo = new Pedido(UUID.randomUUID().toString(), usuarioId, chaveIdempotencia, sessaoId,
                null, null, null, solicitados, null, StatusPedido.PROCESSANDO, null, agora, agora);

        try {
            pedidos.inserir(novo);
        } catch (DuplicadoException corrida) {
            return pedidos.buscarPorUsuarioEChave(usuarioId, chaveIdempotencia).orElseThrow();
        }

        publicador.publicar(PedidoCriado.ROUTING_KEY, new PedidoCriado(novo.id(), sessaoId, itens));
        return novo;
    }

    public void aoRealizado(PedidoRealizado mensagem) {
        Pedido pedido = pedidos.buscarPorId(mensagem.pedidoId()).orElse(null);
        if (pedido == null || pedido.status() != StatusPedido.PROCESSANDO) {
            return; // mensagem repetida ou de pedido desconhecido: nada a fazer
        }

        List<ItemPedido> itens = mensagem.itens().stream()
                .map(i -> new ItemPedido(i.setorId(), i.setorNome(), i.quantidade(), i.precoUnitario(),
                        i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade()))))
                .toList();
        BigDecimal total = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido realizado = new Pedido(pedido.id(), pedido.usuarioId(), pedido.chaveIdempotencia(), pedido.sessaoId(),
                mensagem.eventoId(), mensagem.eventoTitulo(), mensagem.dataEvento(), itens, total,
                StatusPedido.REALIZADO, null, pedido.criadoEm(), Instant.now(clock));

        // Status e ingressos gravados juntos: ou os dois, ou nenhum
        transacao.executar(() -> {
            if (pedidos.atualizarSeProcessando(realizado)) {
                ingressos.salvarTodos(emitirIngressos(realizado));
            }
            return null;
        });
    }

    public void aoCancelado(PedidoCancelado mensagem) {
        pedidos.buscarPorId(mensagem.pedidoId())
                .filter(p -> p.status() == StatusPedido.PROCESSANDO)
                .ifPresent(p -> pedidos.atualizarSeProcessando(new Pedido(p.id(), p.usuarioId(),
                        p.chaveIdempotencia(), p.sessaoId(), p.eventoId(), p.eventoTitulo(), p.dataEvento(),
                        p.itens(), p.total(), StatusPedido.CANCELADO, mensagem.motivo(), p.criadoEm(),
                        Instant.now(clock))));
    }

    public PedidoComIngressos consultar(String usuarioId, String pedidoId) {
        // Pedido de outra pessoa responde igual a pedido inexistente: não revela que existe
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .filter(p -> p.usuarioId().equals(usuarioId))
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado"));

        List<IngressoEmitido> emitidos = pedido.status() == StatusPedido.REALIZADO
                ? ingressos.listarPorPedido(pedido.id())
                : List.of();
        return new PedidoComIngressos(pedido, emitidos);
    }

    public List<Pedido> meusPedidos(String usuarioId) {
        return pedidos.listarPorUsuario(usuarioId);
    }

    public List<IngressoEmitido> meusIngressos(String usuarioId) {
        return ingressos.listarPorUsuario(usuarioId);
    }

    private List<IngressoEmitido> emitirIngressos(Pedido pedido) {
        Instant agora = Instant.now(clock);
        List<IngressoEmitido> emitidos = new ArrayList<>();
        for (ItemPedido item : pedido.itens()) {
            for (int i = 0; i < item.quantidade(); i++) {
                emitidos.add(new IngressoEmitido(UUID.randomUUID().toString(), pedido.id(), pedido.usuarioId(),
                        pedido.sessaoId(), pedido.eventoTitulo(), pedido.dataEvento(), item.setorId(),
                        item.setorNome(), item.precoUnitario(), codigo(item.setorNome()), agora));
            }
        }
        return emitidos;
    }

    private static String codigo(String setorNome) {
        String prefixo = setorNome.toUpperCase().replaceAll("[^A-Z0-9]", "");
        prefixo = prefixo.isEmpty() ? "ING" : prefixo.substring(0, Math.min(5, prefixo.length()));
        return prefixo + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
