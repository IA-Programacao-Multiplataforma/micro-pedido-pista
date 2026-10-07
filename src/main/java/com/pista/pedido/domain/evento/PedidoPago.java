package com.pista.pedido.domain.evento;

public record PedidoPago(String pedidoId, String usuarioId, String sessaoId, String reservaId) {

    public static final String ROUTING_KEY = "pedido.pago";
}
