package com.pista.pedido.spring.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.evento.PedidoCriado;
import com.pista.pedido.domain.service.PedidoService;
import com.pista.pedido.spring.controller.adapter.PedidoControllerAdapter;
import com.pista.pedido.spring.controller.dto.request.CriarPedidoRequest;
import com.pista.pedido.spring.controller.dto.response.CriarPedidoResponse;
import com.pista.pedido.spring.controller.dto.response.IngressoMeuResponse;
import com.pista.pedido.spring.controller.dto.response.PedidoResponse;

import jakarta.validation.Valid;

// Quem é o usuário vem do token: authentication.getName() é o e-mail (subject do JWT).
@RestController
@RequestMapping
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    // INÍCIO DO CICLO: responde 202 com o pedido PROCESSANDO; o estoque decide depois, por mensagem
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/pedidos")
    public CriarPedidoResponse criar(
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveIdempotencia,
            @Valid @RequestBody CriarPedidoRequest request,
            Authentication authentication) {

        List<PedidoCriado.Item> itens = request.itens().stream()
                .map(i -> new PedidoCriado.Item(i.setorId(), i.quantidade()))
                .toList();
        Pedido pedido = service.criar(authentication.getName(), chaveIdempotencia, request.sessaoId(), itens);
        return PedidoControllerAdapter.toCriado(pedido);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResponse consultar(@PathVariable String id, Authentication authentication) {
        return PedidoControllerAdapter.toResponse(service.consultar(authentication.getName(), id));
    }

    @GetMapping("/pedidos/meus")
    public List<PedidoResponse> meusPedidos(Authentication authentication) {
        return service.meusPedidos(authentication.getName()).stream()
                .map(PedidoControllerAdapter::toResponse)
                .toList();
    }

    @GetMapping("/ingressos/meus")
    public List<IngressoMeuResponse> meusIngressos(Authentication authentication) {
        return service.meusIngressos(authentication.getName()).stream()
                .map(PedidoControllerAdapter::toMeu)
                .toList();
    }
}
