package com.pista.pedido.spring.messaging;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// O _id é único: inserir o mesmo eventoId duas vezes falha, e é isso que garante a idempotência.
@Document(value = "eventos_processados")
public record ProcessadoOrmMongo(@Id String eventoId, Instant processadoEm) {
}
