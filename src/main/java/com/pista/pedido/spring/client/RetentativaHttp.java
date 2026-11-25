package com.pista.pedido.spring.client;

import java.time.Duration;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

final class RetentativaHttp {

    private RetentativaHttp() {
    }

    static RetryTemplate criar() {
        RetryPolicy politica = RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofMillis(300))
                .multiplier(2)
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .build();
        return new RetryTemplate(politica);
    }
}
