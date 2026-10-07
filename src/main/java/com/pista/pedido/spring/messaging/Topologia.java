package com.pista.pedido.spring.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pista.pedido.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.pedido.domain.evento.ReservaConfirmada;

// O pedido publica reserva.confirmar e pedido.pago (quem consome declara a própria fila) e consome a fila abaixo.
@Configuration
public class Topologia {

    public static final String EXCHANGE = "pista.eventos";
    public static final String EXCHANGE_MORTAS = "pista.eventos.dlx";

    public static final String FILA_RESERVA_RESULTADO = "pedido.reserva-resultado";

    @Bean
    public TopicExchange exchangePrincipal() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange exchangeMortas() {
        return new DirectExchange(EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaReservaResultado() {
        return comDlq(FILA_RESERVA_RESULTADO);
    }

    @Bean
    public Queue filaReservaResultadoMorta() {
        return QueueBuilder.durable(FILA_RESERVA_RESULTADO + ".dlq").build();
    }

    @Bean
    public Binding ligacaoReservaConfirmada() {
        return BindingBuilder.bind(filaReservaResultado()).to(exchangePrincipal())
                .with(ReservaConfirmada.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoReservaFalhou() {
        return BindingBuilder.bind(filaReservaResultado()).to(exchangePrincipal())
                .with(ReservaConfirmacaoFalhou.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoReservaResultadoMorta() {
        return BindingBuilder.bind(filaReservaResultadoMorta()).to(exchangeMortas())
                .with(FILA_RESERVA_RESULTADO + ".dlq");
    }

    // Auditoria: binding "#" copia todas as mensagens; sem consumidor, elas ficam visíveis no painel do RabbitMQ.
    public static final String FILA_AUDITORIA = "pista.auditoria";

    @Bean
    public Queue filaAuditoria() {
        return QueueBuilder.durable(FILA_AUDITORIA).build();
    }

    @Bean
    public Binding ligacaoAuditoria() {
        return BindingBuilder.bind(filaAuditoria()).to(exchangePrincipal()).with("#");
    }

    private Queue comDlq(String nome) {
        return QueueBuilder.durable(nome)
                .deadLetterExchange(EXCHANGE_MORTAS)
                .deadLetterRoutingKey(nome + ".dlq")
                .build();
    }
}
