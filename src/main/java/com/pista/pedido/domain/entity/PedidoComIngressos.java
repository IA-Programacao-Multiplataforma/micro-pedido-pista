package com.pista.pedido.domain.entity;

import java.util.List;

public record PedidoComIngressos(Pedido pedido, List<IngressoEmitido> ingressos) {
}
