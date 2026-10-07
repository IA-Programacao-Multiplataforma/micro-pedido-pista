package com.pista.pedido.spring.controller.dto.response;

import java.time.LocalDateTime;

public record IngressoMeuResponse(
        String id,
        String pedidoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        String setorNome,
        String codigo
) {
}
