package com.pista.pedido.domain.evento;

import java.util.List;

// Enviado ao Estoque quando o pedido é criado: "este cliente quer estes ingressos desta sessão"
public record PedidoCriado(String pedidoId, String sessaoId, List<Item> itens) {

    public static final String ROUTING_KEY = "pedido.criado";

    public record Item(String setorId, int quantidade) {
    }
}
