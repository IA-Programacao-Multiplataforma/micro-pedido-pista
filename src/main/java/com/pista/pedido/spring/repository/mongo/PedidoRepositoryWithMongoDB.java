package com.pista.pedido.spring.repository.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pista.pedido.spring.repository.orm.PedidoOrmMongo;

public interface PedidoRepositoryWithMongoDB extends MongoRepository<PedidoOrmMongo, String> {
    Optional<PedidoOrmMongo> findFirstByUsuarioIdAndChaveIdempotencia(String usuarioId, String chave);
    List<PedidoOrmMongo> findByUsuarioIdOrderByCriadoEmDesc(String usuarioId);
}
