package com.pista.pedido.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

public record Pedido(
        String id,
        String usuarioId,
        String chaveIdempotencia,
        String sessaoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        String reservaId,
        List<ItemPedido> itens,
        BigDecimal total,
        StatusPedido status,
        String motivo,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public Pedido {
        itens = List.copyOf(itens);
    }
}
