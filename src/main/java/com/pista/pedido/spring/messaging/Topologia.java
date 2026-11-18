package com.pista.pedido.spring.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pista.pedido.domain.evento.PedidoCancelado;
import com.pista.pedido.domain.evento.PedidoRealizado;

// O pedido publica "pedido.criado" (a fila é do estoque) e consome a resposta do estoque na fila abaixo
@Configuration
public class Topologia {

    public static final String EXCHANGE = "pista.eventos";
    public static final String EXCHANGE_MORTAS = "pista.eventos.dlx";

    public static final String FILA_RESPOSTA_ESTOQUE = "pedido.resposta-estoque";
    public static final String FILA_RESPOSTA_ESTOQUE_DLQ = FILA_RESPOSTA_ESTOQUE + ".dlq";

    @Bean
    public TopicExchange exchangePrincipal() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange exchangeMortas() {
        return new DirectExchange(EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaRespostaEstoque() {
        return QueueBuilder.durable(FILA_RESPOSTA_ESTOQUE)
                .deadLetterExchange(EXCHANGE_MORTAS)
                .deadLetterRoutingKey(FILA_RESPOSTA_ESTOQUE_DLQ)
                .build();
    }

    @Bean
    public Queue filaRespostaEstoqueMorta() {
        return QueueBuilder.durable(FILA_RESPOSTA_ESTOQUE_DLQ).build();
    }

    @Bean
    public Binding ligacaoRealizado() {
        return BindingBuilder.bind(filaRespostaEstoque()).to(exchangePrincipal()).with(PedidoRealizado.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoCancelado() {
        return BindingBuilder.bind(filaRespostaEstoque()).to(exchangePrincipal()).with(PedidoCancelado.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoRespostaEstoqueMorta() {
        return BindingBuilder.bind(filaRespostaEstoqueMorta()).to(exchangeMortas()).with(FILA_RESPOSTA_ESTOQUE_DLQ);
    }
}
