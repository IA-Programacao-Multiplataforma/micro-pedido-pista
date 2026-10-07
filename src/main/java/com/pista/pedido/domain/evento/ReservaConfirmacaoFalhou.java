package com.pista.pedido.domain.evento;

public record ReservaConfirmacaoFalhou(String pedidoId, String reservaId, String motivo) {

    public static final String ROUTING_KEY = "reserva.confirmacao-falhou";
}
