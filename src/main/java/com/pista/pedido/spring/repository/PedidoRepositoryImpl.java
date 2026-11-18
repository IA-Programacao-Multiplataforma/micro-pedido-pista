package com.pista.pedido.spring.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.exception.DuplicadoException;
import com.pista.pedido.domain.repository.PedidoRepository;
import com.pista.pedido.spring.repository.adapter.PedidoRepositoryAdapter;
import com.pista.pedido.spring.repository.mongo.PedidoRepositoryWithMongoDB;

@Repository
public class PedidoRepositoryImpl implements PedidoRepository {

    private final PedidoRepositoryWithMongoDB repository;
    private final MongoTemplate mongoTemplate;

    public PedidoRepositoryImpl(PedidoRepositoryWithMongoDB repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void inserir(Pedido pedido) {
        try {
            // insert (e não save): falha se o índice único for violado
            mongoTemplate.insert(PedidoRepositoryAdapter.castEntity(pedido));
        } catch (DuplicateKeyException e) {
            throw new DuplicadoException("Pedido duplicado (mesma Idempotency-Key)");
        }
    }

    @Override
    public Optional<Pedido> buscarPorId(String id) {
        return repository.findById(id).map(PedidoRepositoryAdapter::castOrm);
    }

    @Override
    public Optional<Pedido> buscarPorUsuarioEChave(String usuarioId, String chave) {
        return repository.findFirstByUsuarioIdAndChaveIdempotencia(usuarioId, chave).map(PedidoRepositoryAdapter::castOrm);
    }

    @Override
    public List<Pedido> listarPorUsuario(String usuarioId) {
        return repository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(PedidoRepositoryAdapter::castOrm)
                .toList();
    }

    @Override
    public List<Pedido> listarTodos() {
        return repository.findAll().stream().map(PedidoRepositoryAdapter::castOrm).toList();
    }

    // O banco confere o status e troca numa operação só: se a mesma mensagem chegar duas vezes, só uma grava
    @Override
    public boolean atualizarSeProcessando(Pedido pedido) {
        Query query = new Query(Criteria.where("_id").is(pedido.id()).and("status").is(StatusPedido.PROCESSANDO));
        return mongoTemplate.findAndReplace(query, PedidoRepositoryAdapter.castEntity(pedido)) != null;
    }
}
