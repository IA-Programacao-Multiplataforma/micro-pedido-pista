package com.pista.pedido.spring.controller.adapter;

import java.util.List;

import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.PedidoComIngressos;
import com.pista.pedido.spring.controller.dto.response.CriarPedidoResponse;
import com.pista.pedido.spring.controller.dto.response.IngressoMeuResponse;
import com.pista.pedido.spring.controller.dto.response.PedidoResponse;

public class PedidoControllerAdapter {

    private PedidoControllerAdapter() {}

    public static CriarPedidoResponse toCriado(Pedido pedido) {
        return new CriarPedidoResponse(pedido.id(), pedido.status());
    }

    public static PedidoResponse toResponse(PedidoComIngressos completo) {
        return toResponse(completo.pedido(), completo.ingressos());
    }

    public static PedidoResponse toResponse(Pedido pedido) {
        return toResponse(pedido, List.of());
    }

    private static PedidoResponse toResponse(Pedido pedido, List<IngressoEmitido> ingressos) {
        return new PedidoResponse(
                pedido.id(),
                pedido.status(),
                pedido.motivo(),
                pedido.eventoTitulo(),
                pedido.dataEvento(),
                pedido.total(),
                pedido.itens().stream()
                        .map(i -> new PedidoResponse.ItemResponse(i.setorNome(), i.quantidade(),
                                i.precoUnitario(), i.subtotal()))
                        .toList(),
                ingressos.stream()
                        .map(i -> new PedidoResponse.IngressoResponse(i.id(), i.setorNome(), i.codigo()))
                        .toList()
        );
    }

    public static IngressoMeuResponse toMeu(IngressoEmitido ingresso) {
        return new IngressoMeuResponse(ingresso.id(), ingresso.pedidoId(), ingresso.eventoTitulo(),
                ingresso.dataEvento(), ingresso.setorNome(), ingresso.codigo());
    }
}
