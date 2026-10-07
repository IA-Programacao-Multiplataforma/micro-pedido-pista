package com.pista.pedido.domain.entity;

import java.time.LocalDateTime;

public record InfoSessao(String eventoId, String eventoTitulo, LocalDateTime dataEvento) {
}
