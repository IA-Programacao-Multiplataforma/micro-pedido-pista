package com.pista.pedido.spring.repository.orm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(value = "ingressos")
public record IngressoOrmMongo(
        @Id String id,
        @Indexed String pedidoId,
        @Indexed String usuarioId,
        String sessaoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        String setorId,
        String setorNome,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal valor,
        @Indexed(unique = true) String codigo,
        Instant emitidoEm
) {
}
