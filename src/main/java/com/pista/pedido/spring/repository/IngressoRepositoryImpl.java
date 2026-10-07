package com.pista.pedido.spring.repository;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.spring.repository.adapter.PedidoRepositoryAdapter;
import com.pista.pedido.spring.repository.mongo.IngressoRepositoryWithMongoDB;
import com.pista.pedido.spring.repository.orm.IngressoOrmMongo;

@Repository
public class IngressoRepositoryImpl implements IngressoRepository {

    private final IngressoRepositoryWithMongoDB repository;
    private final MongoTemplate mongoTemplate;

    public IngressoRepositoryImpl(IngressoRepositoryWithMongoDB repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void salvarTodos(List<IngressoEmitido> ingressos) {
        // Pelo MongoTemplate para participar da transação em andamento (junto do pedido e da outbox)
        mongoTemplate.insert(ingressos.stream().map(PedidoRepositoryAdapter::castEntity).toList(),
                IngressoOrmMongo.class);
    }

    @Override
    public List<IngressoEmitido> listarPorPedido(String pedidoId) {
        return repository.findByPedidoIdOrderByCodigoAsc(pedidoId).stream()
                .map(PedidoRepositoryAdapter::castOrm).toList();
    }

    @Override
    public List<IngressoEmitido> listarPorUsuario(String usuarioId) {
        return repository.findByUsuarioIdOrderByDataEventoAscCodigoAsc(usuarioId).stream()
                .map(PedidoRepositoryAdapter::castOrm).toList();
    }
}
