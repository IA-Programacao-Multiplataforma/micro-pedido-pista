package com.pista.pedido.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pista.pedido.domain.entity.RelatorioVendas;
import com.pista.pedido.domain.service.RelatorioService;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/vendas")
    public RelatorioVendas vendas() {
        return service.gerar();
    }
}
