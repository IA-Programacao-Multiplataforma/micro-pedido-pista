package com.pista.pedido.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record IngressoEmitido(
        String id,
        String pedidoId,
        String usuarioId,
        String sessaoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        String setorId,
        String setorNome,
        BigDecimal valor,
        String codigo,
        Instant emitidoEm
) {
}
