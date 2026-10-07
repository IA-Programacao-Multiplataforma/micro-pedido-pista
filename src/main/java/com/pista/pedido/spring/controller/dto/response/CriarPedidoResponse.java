package com.pista.pedido.spring.controller.dto.response;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

public record CriarPedidoResponse(String pedidoId, StatusPedido status) {
}
