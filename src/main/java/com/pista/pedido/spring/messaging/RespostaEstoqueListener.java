package com.pista.pedido.spring.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pista.pedido.domain.evento.PedidoCancelado;
import com.pista.pedido.domain.evento.PedidoRealizado;
import com.pista.pedido.domain.service.PedidoService;

// Só recebe a resposta do estoque e entrega ao serviço; a regra de negócio fica no PedidoService
@Component
public class RespostaEstoqueListener {

    private static final Logger log = LoggerFactory.getLogger(RespostaEstoqueListener.class);

    private final MensagemJson mensagens;
    private final PedidoService service;

    public RespostaEstoqueListener(MensagemJson mensagens, PedidoService service) {
        this.mensagens = mensagens;
        this.service = service;
    }

    @RabbitListener(queues = Topologia.FILA_RESPOSTA_ESTOQUE)
    public void receber(String corpo) {
        MensagemJson.Recebida m = mensagens.ler(corpo);
        log.info("[pedido] recebi {}", m.tipo());

        switch (m.tipo()) {
            case PedidoRealizado.ROUTING_KEY -> service.aoRealizado(mensagens.dados(m, PedidoRealizado.class));
            case PedidoCancelado.ROUTING_KEY -> service.aoCancelado(mensagens.dados(m, PedidoCancelado.class));
            default -> throw new IllegalArgumentException("Tipo de mensagem inesperado: " + m.tipo());
        }
    }
}
