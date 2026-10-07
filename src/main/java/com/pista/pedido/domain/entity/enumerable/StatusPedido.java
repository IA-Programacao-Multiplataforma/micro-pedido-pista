package com.pista.pedido.domain.entity.enumerable;

// PENDENTE e RECUSADO só existem em pedidos antigos, do tempo em que havia pagamento
public enum StatusPedido {
    PENDENTE,
    CONFIRMANDO,
    PAGO,
    RECUSADO,
    CANCELADO
}
