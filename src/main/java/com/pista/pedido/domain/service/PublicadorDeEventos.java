package com.pista.pedido.domain.service;

// A implementação grava na outbox, dentro da transação em andamento: estado e mensagem são gravados juntos ou nenhum dos dois.
public interface PublicadorDeEventos {
    void publicar(String routingKey, Object dados);
}
