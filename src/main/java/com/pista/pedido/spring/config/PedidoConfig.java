package com.pista.pedido.spring.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pista.pedido.domain.client.EstoqueClient;
import com.pista.pedido.domain.client.FilaClient;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;
import com.pista.pedido.domain.service.EventosProcessados;
import com.pista.pedido.domain.service.PedidoService;
import com.pista.pedido.domain.service.PublicadorDeEventos;
import com.pista.pedido.domain.service.RelatorioService;
import com.pista.pedido.domain.service.UnidadeDeTrabalho;

@Configuration
@EnableScheduling
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
    public PedidoService pedidoService(
            PedidoRepository pedidos,
            IngressoRepository ingressos,
            FilaClient fila,
            EstoqueClient estoque,
            PublicadorDeEventos publicador,
            EventosProcessados processados,
            UnidadeDeTrabalho unidade,
            Clock clock,
            @Value("${pedido.tempo-minimo-pagar-segundos}") long segundosMinimos) {
        return new PedidoService(pedidos, ingressos, fila, estoque, publicador, processados, unidade, clock,
                Duration.ofSeconds(segundosMinimos));
    }
}
