package com.pista.pedido.spring.repository.orm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.pista.pedido.domain.entity.enumerable.StatusPedido;

// Índice único (usuarioId, chaveIdempotencia): o mesmo clique nunca cria dois pedidos
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
        List<ItemPedidoOrmMongo> itens,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal total,
        StatusPedido status,
        String motivo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
