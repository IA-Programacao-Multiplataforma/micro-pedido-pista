package com.pista.pedido.spring.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pista.pedido.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.pedido.domain.evento.ReservaConfirmada;
import com.pista.pedido.domain.service.PedidoService;

// Se um método lançar exceção, o Spring tenta de novo (3x) e depois manda para a DLQ.
@Component
public class PedidoListeners {

    private static final Logger log = LoggerFactory.getLogger(PedidoListeners.class);

    private final MensagemJson mensagens;
    private final PedidoService service;

    public PedidoListeners(MensagemJson mensagens, PedidoService service) {
        this.mensagens = mensagens;
        this.service = service;
    }

    @RabbitListener(queues = Topologia.FILA_RESERVA_RESULTADO)
    public void resultadoDaReserva(String corpo) {
        MensagemJson.Recebida m = mensagens.ler(corpo);
        log.info("[saga] recebi {}: eventoId={}", m.tipo(), m.eventoId());

        switch (m.tipo()) {
            case ReservaConfirmada.ROUTING_KEY ->
                    service.aoReservaConfirmada(m.eventoId(), mensagens.dados(m, ReservaConfirmada.class));
            case ReservaConfirmacaoFalhou.ROUTING_KEY ->
                    service.aoReservaFalhou(m.eventoId(), mensagens.dados(m, ReservaConfirmacaoFalhou.class));
            default -> throw new IllegalArgumentException("Tipo de mensagem inesperado: " + m.tipo());
        }
    }
}
