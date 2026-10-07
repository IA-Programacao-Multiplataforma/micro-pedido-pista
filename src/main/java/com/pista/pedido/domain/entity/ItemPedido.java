package com.pista.pedido.domain.entity;

import java.math.BigDecimal;

public record ItemPedido(
        String setorId,
        String setorNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}
