package com.pista.pedido.domain.exception;

public class SacolaJaEmPedidoException extends ConflitoException {

    private final String pedidoId;

    public SacolaJaEmPedidoException(String pedidoId) {
        super("Esta sacola já está em um pedido");
        this.pedidoId = pedidoId;
    }

    public String pedidoId() {
        return pedidoId;
    }
}
