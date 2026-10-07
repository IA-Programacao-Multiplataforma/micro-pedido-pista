package com.pista.pedido.spring.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarPedidoRequest(
        @NotBlank(message = "Sessão é obrigatória") String sessaoId
) {
}
