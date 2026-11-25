package com.pista.pedido.spring.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class RetentativaHttpTest {

    private final RetryTemplate retentativa = RetentativaHttp.criar();
    private final AtomicInteger chamadas = new AtomicInteger();

    @Test
    void repeteFalhaDeRedeEDevolveORespostaQuandoOServicoVolta() {
        String resposta = retentativa.invoke(() -> {
            if (chamadas.incrementAndGet() < 3) {
                throw new ResourceAccessException("timeout");
            }
            return "ok";
        });

        assertThat(resposta).isEqualTo("ok");
        assertThat(chamadas).hasValue(3);
    }

    @Test
    void desisteDepoisDeTresTentativasERelancaAExcecaoOriginal() {
        assertThatThrownBy(() -> retentativa.invoke(() -> {
            chamadas.incrementAndGet();
            throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "503", null, null, null);
        })).isInstanceOf(HttpServerErrorException.class);

        assertThat(chamadas).hasValue(3);
    }

    @Test
    void naoRepeteErro4xx() {
        assertThatThrownBy(() -> retentativa.invoke(() -> {
            chamadas.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "404", null, null, null);
        })).isInstanceOf(HttpClientErrorException.class);

        assertThat(chamadas).hasValue(1);
    }
}
