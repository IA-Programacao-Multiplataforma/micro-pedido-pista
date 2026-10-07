package com.pista.pedido.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Sacola(
        String reservaId,
        String sessaoId,
        Instant expiraEm,
        List<ItemPedido> itens,
        BigDecimal total
) {
}
