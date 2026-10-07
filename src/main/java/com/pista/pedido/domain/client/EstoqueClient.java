package com.pista.pedido.domain.client;

import com.pista.pedido.domain.entity.InfoSessao;

public interface EstoqueClient {

    InfoSessao buscarSessao(String sessaoId);
}
