package com.pista.pedido.domain.client;

import com.pista.pedido.domain.entity.Sacola;

public interface FilaClient {

    Sacola buscarSacola(String credencial, String sessaoId);
}
