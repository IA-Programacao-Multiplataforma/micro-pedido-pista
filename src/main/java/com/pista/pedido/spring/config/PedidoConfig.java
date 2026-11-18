package com.pista.pedido.spring.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;
import com.pista.pedido.domain.service.PedidoService;
import com.pista.pedido.domain.service.PublicadorDeEventos;
import com.pista.pedido.domain.service.RelatorioService;
import com.pista.pedido.domain.service.UnidadeDeTrabalho;

@Configuration
public class PedidoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RelatorioService relatorioService(PedidoRepository pedidos) {
        return new RelatorioService(pedidos);
    }

    @Bean
    public PedidoService pedidoService(PedidoRepository pedidos, IngressoRepository ingressos,
                                       PublicadorDeEventos publicador, UnidadeDeTrabalho transacao, Clock clock) {
        return new PedidoService(pedidos, ingressos, publicador, transacao, clock);
    }
}
