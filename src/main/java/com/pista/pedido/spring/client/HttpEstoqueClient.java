package com.pista.pedido.spring.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.pista.pedido.domain.client.EstoqueClient;
import com.pista.pedido.domain.entity.InfoSessao;
import com.pista.pedido.domain.exception.NaoEncontradoException;
import com.pista.pedido.domain.exception.ServicoIndisponivelException;

@Component
public class HttpEstoqueClient implements EstoqueClient {

    private final RestClient http;
    private final RetryTemplate retentativa = RetentativaHttp.criar();

    public HttpEstoqueClient(@Value("${estoque.url}") String url) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    @Override
    public InfoSessao buscarSessao(String sessaoId) {
        try {
            SessaoJson json = retentativa.invoke(() ->
                    http.get().uri("/sessoes/{id}", sessaoId).retrieve().body(SessaoJson.class));
            return new InfoSessao(json.eventoId(), json.eventoTitulo(), json.dataEvento());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new NaoEncontradoException("Sessão não encontrada");
            }
            throw new ServicoIndisponivelException("Serviço de estoque indisponível. Tente novamente");
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException("Serviço de estoque indisponível. Tente novamente");
        }
    }

    record SessaoJson(String eventoId, String eventoTitulo, LocalDateTime dataEvento) {
    }
}
