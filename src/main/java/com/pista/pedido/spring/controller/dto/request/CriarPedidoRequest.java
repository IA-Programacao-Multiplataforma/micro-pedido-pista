package com.pista.pedido.spring.controller.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// O cliente escolheu o show (sessão) e quantos ingressos de cada setor
public record CriarPedidoRequest(
        @NotBlank(message = "Sessão é obrigatória") String sessaoId,
        @NotEmpty(message = "Escolha pelo menos um ingresso") @Valid List<Item> itens
) {

    public record Item(
            @NotBlank(message = "Setor é obrigatório") String setorId,
            @Min(value = 1, message = "Quantidade deve ser maior que zero") int quantidade
    ) {
    }
}
