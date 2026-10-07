package com.pista.pedido.spring.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.exception.DuplicadoException;
import com.pista.pedido.domain.repository.PedidoRepository;
import com.pista.pedido.spring.repository.adapter.PedidoRepositoryAdapter;
import com.pista.pedido.spring.repository.mongo.PedidoRepositoryWithMongoDB;
import com.pista.pedido.spring.repository.orm.PedidoOrmMongo;

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
            // insert (e não save): falha se algum índice único for violado
            mongoTemplate.insert(PedidoRepositoryAdapter.castEntity(pedido));
        } catch (DuplicateKeyException e) {
            throw new DuplicadoException("Pedido duplicado (mesma Idempotency-Key ou mesma reserva)");
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
    public Optional<Pedido> buscarAtivoPorReservaId(String reservaId) {
        return repository.findFirstByReservaIdAndReservaEmUsoTrue(reservaId).map(PedidoRepositoryAdapter::castOrm);
    }

    @Override
    public List<Pedido> listarPorUsuario(String usuarioId) {
        return repository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(PedidoRepositoryAdapter::castOrm)
                .toList();
    }

    @Override
    public List<Pedido> listarTodos() {
        return repository.findAll().stream()
                .map(PedidoRepositoryAdapter::castOrm)
                .toList();
    }

    // O banco confere o status e troca numa operação só: de duas mensagens simultâneas, só uma consegue.
    @Override
    public boolean transicionar(String id, StatusPedido de, StatusPedido para, String motivo, Instant agora) {
        Query query = new Query(Criteria.where("_id").is(id).and("status").is(de));
        Update update = new Update()
                .set("status", para)
                .set("motivo", motivo)
                .set("reservaEmUso", PedidoRepositoryAdapter.usaReserva(para))
                .set("atualizadoEm", agora);
        return mongoTemplate.updateFirst(query, update, PedidoOrmMongo.class).getModifiedCount() == 1;
    }
}
