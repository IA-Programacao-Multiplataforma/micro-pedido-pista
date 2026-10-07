package com.pista.pedido.spring.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

public record PedidoResponse(
        String id,
        StatusPedido status,
        String motivo,
        String eventoTitulo,
        LocalDateTime dataEvento,
        BigDecimal total,
        List<ItemResponse> itens,
        List<IngressoResponse> ingressos
) {

    public record ItemResponse(String setorNome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    public record IngressoResponse(String id, String setorNome, String codigo) {
    }
}
