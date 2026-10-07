package com.pista.pedido.spring.repository.orm;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record ItemPedidoOrmMongo(
        String setorId,
        String setorNome,
        int quantidade,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal precoUnitario,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal subtotal
) {
}
