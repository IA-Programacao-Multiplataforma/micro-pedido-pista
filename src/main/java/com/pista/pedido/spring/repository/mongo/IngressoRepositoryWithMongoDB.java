package com.pista.pedido.spring.repository.mongo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pista.pedido.spring.repository.orm.IngressoOrmMongo;

public interface IngressoRepositoryWithMongoDB extends MongoRepository<IngressoOrmMongo, String> {
    List<IngressoOrmMongo> findByPedidoIdOrderByCodigoAsc(String pedidoId);
    List<IngressoOrmMongo> findByUsuarioIdOrderByDataEventoAscCodigoAsc(String usuarioId);
}
