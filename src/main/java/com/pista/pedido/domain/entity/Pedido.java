package com.pista.pedido.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

// Enquanto PROCESSANDO, só se sabe sessão, setores e quantidades; título, preços e total chegam com o REALIZADO
public record Pedido(
        String id,
        String usuarioId,
        String chaveIdempotencia,
        String sessaoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
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
