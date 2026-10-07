package com.pista.pedido.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;

public interface PedidoRepository {

    void inserir(Pedido pedido);

    Optional<Pedido> buscarPorId(String id);
    Optional<Pedido> buscarPorUsuarioEChave(String usuarioId, String chave);
    Optional<Pedido> buscarAtivoPorReservaId(String reservaId);
    List<Pedido> listarPorUsuario(String usuarioId);
    List<Pedido> listarTodos();

    // Troca o status só se o pedido ainda estiver em "de"; true = esta chamada fez a troca.
    boolean transicionar(String id, StatusPedido de, StatusPedido para, String motivo, Instant agora);
}
