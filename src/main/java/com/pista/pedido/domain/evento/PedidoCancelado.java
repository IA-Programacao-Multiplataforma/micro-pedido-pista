package com.pista.pedido.domain.evento;

// Recebido do Estoque: não havia ingressos; nada foi vendido
public record PedidoCancelado(String pedidoId, String motivo) {

    public static final String ROUTING_KEY = "pedido.cancelado";
}
