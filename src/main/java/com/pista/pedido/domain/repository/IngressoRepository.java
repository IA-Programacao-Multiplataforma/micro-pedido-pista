package com.pista.pedido.domain.repository;

import java.util.List;

import com.pista.pedido.domain.entity.IngressoEmitido;

public interface IngressoRepository {
    void salvarTodos(List<IngressoEmitido> ingressos);
    List<IngressoEmitido> listarPorPedido(String pedidoId);
    List<IngressoEmitido> listarPorUsuario(String usuarioId);
}
