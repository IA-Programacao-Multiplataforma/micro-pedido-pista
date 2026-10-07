package com.pista.pedido.spring.repository.orm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

// Índices únicos contra concorrência (duplo clique, duas abas):
//  1) (usuarioId, chaveIdempotencia): o mesmo clique não cria dois pedidos
//  2) reservaId, só entre pedidos com reservaEmUso = true: uma reserva vira um único pedido ativo
@Document(value = "pedidos")
@CompoundIndex(name = "usuario_chave_unico", def = "{ 'usuarioId': 1, 'chaveIdempotencia': 1 }", unique = true)
public record PedidoOrmMongo(
        @Id String id,
        String usuarioId,
        String chaveIdempotencia,
        String sessaoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        @Indexed(unique = true, partialFilter = "{ 'reservaEmUso': true }") String reservaId,
        boolean reservaEmUso,
        List<ItemPedidoOrmMongo> itens,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal total,
        StatusPedido status,
        String motivo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
