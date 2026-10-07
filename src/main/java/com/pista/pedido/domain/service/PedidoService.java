package com.pista.pedido.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.pista.pedido.domain.client.EstoqueClient;
import com.pista.pedido.domain.client.FilaClient;
import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.InfoSessao;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.PedidoComIngressos;
import com.pista.pedido.domain.entity.Sacola;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.evento.PedidoPago;
import com.pista.pedido.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.pedido.domain.evento.ReservaConfirmada;
import com.pista.pedido.domain.evento.ReservaConfirmar;
import com.pista.pedido.domain.exception.ConflitoException;
import com.pista.pedido.domain.exception.DuplicadoException;
import com.pista.pedido.domain.exception.MensagemDuplicadaException;
import com.pista.pedido.domain.exception.NaoEncontradoException;
import com.pista.pedido.domain.exception.RegraDeNegocioException;
import com.pista.pedido.domain.exception.SacolaJaEmPedidoException;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;

public class PedidoService {

    private final PedidoRepository pedidos;
    private final IngressoRepository ingressos;
    private final FilaClient fila;
    private final EstoqueClient estoque;
    private final PublicadorDeEventos publicador;
    private final EventosProcessados processados;
    private final UnidadeDeTrabalho unidade;
    private final Clock clock;
    private final Duration tempoMinimoParaPagar;

    public PedidoService(PedidoRepository pedidos, IngressoRepository ingressos, FilaClient fila,
                         EstoqueClient estoque, PublicadorDeEventos publicador, EventosProcessados processados,
                         UnidadeDeTrabalho unidade, Clock clock, Duration tempoMinimoParaPagar) {
        this.pedidos = pedidos;
        this.ingressos = ingressos;
        this.fila = fila;
        this.estoque = estoque;
        this.publicador = publicador;
        this.processados = processados;
        this.unidade = unidade;
        this.clock = clock;
        this.tempoMinimoParaPagar = tempoMinimoParaPagar;
    }

    public Pedido criar(String usuarioId, String credencial, String chaveIdempotencia, String sessaoId) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
            throw new RegraDeNegocioException("Idempotency-Key é obrigatório");
        }

        // Mesma Idempotency-Key = mesmo clique: devolve o mesmo pedido, sem criar outro
        var repetido = pedidos.buscarPorUsuarioEChave(usuarioId, chaveIdempotencia);
        if (repetido.isPresent()) {
            return repetido.get();
        }

        Sacola sacola = buscarSacola(credencial, sessaoId);

        Instant agora = Instant.now(clock);
        if (Duration.between(agora, sacola.expiraEm()).compareTo(tempoMinimoParaPagar) < 0) {
            throw new ConflitoException("Tempo insuficiente para confirmar. Entre na fila novamente");
        }

        var emUso = pedidos.buscarAtivoPorReservaId(sacola.reservaId());
        if (emUso.isPresent()) {
            Pedido existente = emUso.get();
            // Mesmo usuário e mesma chave: é o mesmo clique chegando junto, não uma nova tentativa
            if (existente.usuarioId().equals(usuarioId) && existente.chaveIdempotencia().equals(chaveIdempotencia)) {
                return existente;
            }
            throw new SacolaJaEmPedidoException(existente.id());
        }

        InfoSessao sessao = estoque.buscarSessao(sessaoId);

        Pedido novo = new Pedido(UUID.randomUUID().toString(), usuarioId, chaveIdempotencia, sessaoId,
                sessao.eventoId(), sessao.eventoTitulo(), sessao.dataEvento(), sacola.reservaId(),
                sacola.itens(), sacola.total(), StatusPedido.CONFIRMANDO, null, agora, agora);

        try {
            // O pedido e a mensagem reserva.confirmar nascem na mesma transação
            unidade.executar(() -> {
                pedidos.inserir(novo);
                publicador.publicar(ReservaConfirmar.ROUTING_KEY, new ReservaConfirmar(novo.id(), novo.reservaId()));
                return null;
            });
        } catch (DuplicadoException corrida) {
            // Dois cliques simultâneos o índice único do banco deixou só um passar
            return pedidos.buscarPorUsuarioEChave(usuarioId, chaveIdempotencia)
                    .orElseGet(() -> {
                        var dono = pedidos.buscarAtivoPorReservaId(sacola.reservaId());
                        throw dono.isPresent()
                                ? new SacolaJaEmPedidoException(dono.get().id())
                                : new ConflitoException("Esta sacola já está em um pedido");
                    });
        }
        return novo;
    }

    public PedidoComIngressos consultar(String usuarioId, String pedidoId) {
        // Pedido de outra pessoa responde igual a pedido inexistente: não revela que existe
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .filter(p -> p.usuarioId().equals(usuarioId))
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado"));

        List<IngressoEmitido> emitidos = pedido.status() == StatusPedido.PAGO
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

    public void aoReservaConfirmada(String eventoId, ReservaConfirmada mensagem) {
        unicaVez(eventoId, () -> {
            boolean mudou = pedidos.transicionar(mensagem.pedidoId(), StatusPedido.CONFIRMANDO,
                    StatusPedido.PAGO, null, Instant.now(clock));
            if (mudou) {
                Pedido pedido = pedidos.buscarPorId(mensagem.pedidoId()).orElseThrow();
                ingressos.salvarTodos(emitirIngressos(pedido));
                publicador.publicar(PedidoPago.ROUTING_KEY, new PedidoPago(pedido.id(), pedido.usuarioId(),
                        pedido.sessaoId(), pedido.reservaId()));
            }
        });
    }

    public void aoReservaFalhou(String eventoId, ReservaConfirmacaoFalhou mensagem) {
        unicaVez(eventoId, () ->
                pedidos.transicionar(mensagem.pedidoId(), StatusPedido.CONFIRMANDO,
                        StatusPedido.CANCELADO, mensagem.motivo(), Instant.now(clock)));
    }

    private void unicaVez(String eventoId, Runnable efeito) {
        if (processados.jaProcessado(eventoId)) {
            return;
        }
        try {
            unidade.executar(() -> {
                processados.registrar(eventoId);
                efeito.run();
                return null;
            });
        } catch (MensagemDuplicadaException duplicada) {
            // Outra entrega da mesma mensagem venceu a corrida: o trabalho já foi feito
        }
    }

    private Sacola buscarSacola(String credencial, String sessaoId) {
        try {
            return fila.buscarSacola(credencial, sessaoId);
        } catch (NaoEncontradoException vazia) {
            throw new ConflitoException("Sua sacola está vazia ou o tempo acabou");
        }
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
