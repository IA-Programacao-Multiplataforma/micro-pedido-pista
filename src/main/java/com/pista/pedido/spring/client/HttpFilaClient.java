package com.pista.pedido.spring.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.pista.pedido.domain.client.FilaClient;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Sacola;
import com.pista.pedido.domain.exception.NaoEncontradoException;
import com.pista.pedido.domain.exception.ServicoIndisponivelException;

@Component
public class HttpFilaClient implements FilaClient {

    private final RestClient http;
    private final RetryTemplate retentativa = RetentativaHttp.criar();

    public HttpFilaClient(@Value("${fila.url}") String url) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    @Override
    public Sacola buscarSacola(String credencial, String sessaoId) {
        try {
            SacolaJson json = retentativa.invoke(() -> http.get()
                    .uri("/sessoes/{id}/sacola", sessaoId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credencial)
                    .retrieve()
                    .body(SacolaJson.class));

            return new Sacola(json.reservaId(), json.sessaoId(), json.expiraEm(),
                    json.itens().stream()
                            .map(i -> new ItemPedido(i.setorId(), i.setorNome(), i.quantidade(),
                                    i.precoUnitario(), i.subtotal()))
                            .toList(),
                    json.total());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new NaoEncontradoException("Sacola vazia");
            }
            throw new ServicoIndisponivelException("Serviço de fila indisponível. Tente novamente");
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException("Serviço de fila indisponível. Tente novamente");
        }
    }

    record SacolaJson(String reservaId, String sessaoId, Instant expiraEm, List<ItemJson> itens, BigDecimal total) {
    }

    record ItemJson(String setorId, String setorNome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }
}
