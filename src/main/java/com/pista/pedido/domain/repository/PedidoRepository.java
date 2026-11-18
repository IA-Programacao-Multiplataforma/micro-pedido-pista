package com.pista.pedido.domain.repository;

import java.util.List;
import java.util.Optional;

import com.pista.pedido.domain.entity.Pedido;

public interface PedidoRepository {

    void inserir(Pedido pedido);

    Optional<Pedido> buscarPorId(String id);
    Optional<Pedido> buscarPorUsuarioEChave(String usuarioId, String chave);
    List<Pedido> listarPorUsuario(String usuarioId);
    List<Pedido> listarTodos();

    // Grava a nova versão só se o pedido ainda estiver PROCESSANDO; false = já tinha sido atualizado
    boolean atualizarSeProcessando(Pedido pedido);
}
