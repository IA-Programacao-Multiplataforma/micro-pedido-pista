package com.pista.pedido.spring.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pista.pedido.domain.service.PublicadorDeEventos;

// Só envia a mensagem para o RabbitMQ
@Component
public class RabbitPublicador implements PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublicador.class);

    private final RabbitTemplate rabbit;
    private final MensagemJson mensagens;

    public RabbitPublicador(RabbitTemplate rabbit, MensagemJson mensagens) {
        this.rabbit = rabbit;
        this.mensagens = mensagens;
    }

    @Override
    public void publicar(String routingKey, Object dados) {
        rabbit.convertAndSend(Topologia.EXCHANGE, routingKey, mensagens.montar(routingKey, dados).json());
        log.info("[pedido] enviei {}", routingKey);
    }
}
