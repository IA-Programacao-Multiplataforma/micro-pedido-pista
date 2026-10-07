package com.pista.pedido.domain.service;

// O RabbitMQ pode entregar a mesma mensagem mais de uma vez: aqui ficam os eventoIds já tratados.
public interface EventosProcessados {

    boolean jaProcessado(String eventoId);

    void registrar(String eventoId);
}
