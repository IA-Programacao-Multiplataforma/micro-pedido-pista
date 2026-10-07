package com.pista.pedido.domain.service;

import java.util.function.Supplier;

public interface UnidadeDeTrabalho {
    <T> T executar(Supplier<T> trabalho);
}
