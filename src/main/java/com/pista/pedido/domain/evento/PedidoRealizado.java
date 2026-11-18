package com.pista.pedido.domain.evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Recebido do Estoque: havia ingressos e a baixa foi feita (traz nomes e preços)
public record PedidoRealizado(
        String pedidoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        List<Item> itens
) {

    public static final String ROUTING_KEY = "pedido.realizado";

    public record Item(String setorId, String setorNome, int quantidade, BigDecimal precoUnitario) {
    }
}
