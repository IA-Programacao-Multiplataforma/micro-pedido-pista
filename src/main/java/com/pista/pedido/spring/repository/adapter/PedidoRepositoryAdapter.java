package com.pista.pedido.spring.repository.adapter;

import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.spring.repository.orm.IngressoOrmMongo;
import com.pista.pedido.spring.repository.orm.ItemPedidoOrmMongo;
import com.pista.pedido.spring.repository.orm.PedidoOrmMongo;

public class PedidoRepositoryAdapter {
    private PedidoRepositoryAdapter() {}

    public static Pedido castOrm(PedidoOrmMongo orm) {
        return new Pedido(orm.id(), orm.usuarioId(), orm.chaveIdempotencia(), orm.sessaoId(), orm.eventoId(),
                orm.eventoTitulo(), orm.dataEvento(),
                orm.itens().stream()
                        .map(i -> new ItemPedido(i.setorId(), i.setorNome(), i.quantidade(), i.precoUnitario(), i.subtotal()))
                        .toList(),
                orm.total(), orm.status(), orm.motivo(), orm.criadoEm(), orm.atualizadoEm());
    }

    public static PedidoOrmMongo castEntity(Pedido entity) {
        return new PedidoOrmMongo(entity.id(), entity.usuarioId(), entity.chaveIdempotencia(), entity.sessaoId(),
                entity.eventoId(), entity.eventoTitulo(), entity.dataEvento(),
                entity.itens().stream()
                        .map(i -> new ItemPedidoOrmMongo(i.setorId(), i.setorNome(), i.quantidade(), i.precoUnitario(), i.subtotal()))
                        .toList(),
                entity.total(), entity.status(), entity.motivo(), entity.criadoEm(), entity.atualizadoEm());
    }

    public static IngressoEmitido castOrm(IngressoOrmMongo orm) {
        return new IngressoEmitido(orm.id(), orm.pedidoId(), orm.usuarioId(), orm.sessaoId(), orm.eventoTitulo(),
                orm.dataEvento(), orm.setorId(), orm.setorNome(), orm.valor(), orm.codigo(), orm.emitidoEm());
    }

    public static IngressoOrmMongo castEntity(IngressoEmitido entity) {
        return new IngressoOrmMongo(entity.id(), entity.pedidoId(), entity.usuarioId(), entity.sessaoId(),
                entity.eventoTitulo(), entity.dataEvento(), entity.setorId(), entity.setorNome(), entity.valor(),
                entity.codigo(), entity.emitidoEm());
    }
}
