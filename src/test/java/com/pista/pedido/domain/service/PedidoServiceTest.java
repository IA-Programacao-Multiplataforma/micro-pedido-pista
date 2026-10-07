package com.pista.pedido.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pista.pedido.domain.client.EstoqueClient;
import com.pista.pedido.domain.client.FilaClient;
import com.pista.pedido.domain.entity.IngressoEmitido;
import com.pista.pedido.domain.entity.InfoSessao;
import com.pista.pedido.domain.entity.ItemPedido;
import com.pista.pedido.domain.entity.Pedido;
import com.pista.pedido.domain.entity.Sacola;
import com.pista.pedido.domain.entity.enumerable.StatusPedido;
import com.pista.pedido.domain.evento.PedidoPago;
import com.pista.pedido.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.pedido.domain.evento.ReservaConfirmada;
import com.pista.pedido.domain.evento.ReservaConfirmar;
import com.pista.pedido.domain.exception.ConflitoException;
import com.pista.pedido.domain.exception.DuplicadoException;
import com.pista.pedido.domain.exception.MensagemDuplicadaException;
import com.pista.pedido.domain.exception.NaoEncontradoException;
import com.pista.pedido.domain.exception.RegraDeNegocioException;
import com.pista.pedido.domain.exception.SacolaJaEmPedidoException;
import com.pista.pedido.domain.exception.ServicoIndisponivelException;
import com.pista.pedido.domain.repository.IngressoRepository;
import com.pista.pedido.domain.repository.PedidoRepository;

class PedidoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-01T12:00:00Z");

    private FakePedidos pedidos;
    private FakeIngressos ingressos;
    private FakeFila fila;
    private FakePublicador publicador;
    private FakeProcessados processados;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        pedidos = new FakePedidos();
        ingressos = new FakeIngressos();
        fila = new FakeFila();
        publicador = new FakePublicador();
        processados = new FakeProcessados();
        service = new PedidoService(pedidos, ingressos, fila, new FakeEstoque(), publicador, processados,
                new SemTransacao(), Clock.fixed(AGORA, ZoneOffset.UTC), Duration.ofSeconds(60));
    }

    private Pedido criar(String chave) {
        return service.criar("ana@x.com", "token-da-ana", chave, "ses-1");
    }

    @Test
    void confirmarASacola_criaPedidoConfirmandoEPedeAoEstoqueParaConfirmarAReserva() {
        Pedido p = criar("chave-1");

        assertThat(p.status()).isEqualTo(StatusPedido.CONFIRMANDO);
        assertThat(p.total()).isEqualByComparingTo("540.00");
        assertThat(p.reservaId()).isEqualTo("res-1");
        assertThat(p.eventoTitulo()).isEqualTo("Festival Aurora");

        assertThat(publicador.chaves()).containsExactly("reserva.confirmar");
        ReservaConfirmar cmd = (ReservaConfirmar) publicador.dados(0);
        assertThat(cmd.reservaId()).isEqualTo("res-1");
        assertThat(cmd.pedidoId()).isEqualTo(p.id());
    }

    @Test
    void usaOTokenDoClienteParaLerASacola() {
        criar("chave-1");

        assertThat(fila.ultimaCredencial).isEqualTo("token-da-ana");
    }

    @Test
    void mesmoCliqueDuasVezes_devolveOMesmoPedidoSemCriarOutro() {
        Pedido primeiro = criar("chave-1");
        Pedido segundo = criar("chave-1");

        assertThat(segundo.id()).isEqualTo(primeiro.id());
        assertThat(pedidos.total()).isEqualTo(1);
        assertThat(publicador.chaves()).hasSize(1);
    }

    @Test
    void dezCliquesSimultaneosComAMesmaChave_geramUmUnicoPedidoEUmaUnicaMensagem() throws Exception {
        int cliques = 10;
        ExecutorService pool = Executors.newFixedThreadPool(cliques);
        CountDownLatch largada = new CountDownLatch(1);
        List<java.util.concurrent.Future<Pedido>> resultados = new ArrayList<>();

        for (int i = 0; i < cliques; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return criar("mesma-chave");
            }));
        }
        largada.countDown();

        Set<String> ids = new HashSet<>();
        for (var r : resultados) {
            ids.add(r.get(10, TimeUnit.SECONDS).id());
        }
        pool.shutdown();

        assertThat(ids).hasSize(1);
        assertThat(pedidos.total()).isEqualTo(1);
        assertThat(publicador.chaves()).hasSize(1);
    }

    @Test
    void cliqueQueChegaDepoisDoPedidoExistirMasAntesDeEnxergarAChave_recebeOMesmoPedido() {
        Pedido primeiro = criar("mesma-chave");
        pedidos.naoEnxergarChavePorEnquanto = true;

        Pedido segundo = criar("mesma-chave");

        assertThat(segundo.id()).isEqualTo(primeiro.id());
        assertThat(pedidos.total()).isEqualTo(1);
        assertThat(publicador.chaves()).hasSize(1);
    }

    @Test
    void duasAbasComChavesDiferentes_apenasUmaFicaComASacola() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);

        var a = pool.submit(() -> { largada.await(); return tentar("chave-A"); });
        var b = pool.submit(() -> { largada.await(); return tentar("chave-B"); });
        largada.countDown();
        String ra = a.get(10, TimeUnit.SECONDS);
        String rb = b.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(List.of(ra, rb)).containsExactlyInAnyOrder("criou", "recusou");
        assertThat(pedidos.total()).isEqualTo(1);
    }

    private String tentar(String chave) {
        try {
            criar(chave);
            return "criou";
        } catch (ConflitoException e) {
            return "recusou";
        }
    }

    @Test
    void sacolaQueJaVirouPedidoNaoGeraOutro_eInformaOPedidoExistente() {
        Pedido primeiro = criar("chave-1");

        assertThatThrownBy(() -> criar("chave-2"))
                .isInstanceOf(SacolaJaEmPedidoException.class)
                .hasMessage("Esta sacola já está em um pedido")
                .satisfies(e -> assertThat(((SacolaJaEmPedidoException) e).pedidoId()).isEqualTo(primeiro.id()));
    }

    @Test
    void semSacolaOuSacolaExpirada_recusaComMensagemClara() {
        fila.sacola = null;

        assertThatThrownBy(() -> criar("chave-1"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Sua sacola está vazia ou o tempo acabou");
        assertThat(pedidos.total()).isZero();
    }

    @Test
    void menosDeSessentaSegundosParaASacolaVencer_recusa() {
        fila.sacola = sacola(AGORA.plusSeconds(59));

        assertThatThrownBy(() -> criar("chave-1"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Tempo insuficiente para confirmar. Entre na fila novamente");
    }

    @Test
    void exatamenteSessentaSegundos_aindaAceita() {
        fila.sacola = sacola(AGORA.plusSeconds(60));

        assertThat(criar("chave-1").status()).isEqualTo(StatusPedido.CONFIRMANDO);
    }

    @Test
    void filaForaDoAr_naoCriaPedido() {
        fila.indisponivel = true;

        assertThatThrownBy(() -> criar("chave-1")).isInstanceOf(ServicoIndisponivelException.class);
        assertThat(pedidos.total()).isZero();
    }

    @Test
    void exigeAChaveDeIdempotencia() {
        assertThatThrownBy(() -> service.criar("ana@x.com", "t", " ", "ses-1"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessage("Idempotency-Key é obrigatório");
        assertThat(pedidos.total()).isZero();
    }

    @Test
    void reservaConfirmada_viraPagoEmiteUmIngressoPorUnidadeEAvisaAFila() {
        Pedido p = criar("k");
        publicador.limpar();

        service.aoReservaConfirmada("evt-2", new ReservaConfirmada(p.id(), "res-1"));

        assertThat(pedidos.status(p.id())).isEqualTo(StatusPedido.PAGO);
        List<IngressoEmitido> emitidos = ingressos.listarPorPedido(p.id());
        assertThat(emitidos).hasSize(3);
        assertThat(emitidos.stream().filter(i -> i.setorNome().equals("Pista"))).hasSize(2);
        assertThat(emitidos).allSatisfy(i -> assertThat(i.codigo()).matches("[A-Z0-9]{1,5}-[A-F0-9]{6}"));
        assertThat(emitidos.stream().map(IngressoEmitido::codigo).distinct()).hasSize(3);
        assertThat(publicador.chaves()).containsExactly("pedido.pago");
        PedidoPago pago = (PedidoPago) publicador.dados(0);
        assertThat(pago.usuarioId()).isEqualTo("ana@x.com");
        assertThat(pago.sessaoId()).isEqualTo("ses-1");
    }

    @Test
    void reservaNaoConfirmada_cancelaOPedido() {
        Pedido p = criar("k");
        publicador.limpar();

        service.aoReservaFalhou("evt-2", new ReservaConfirmacaoFalhou(p.id(), "res-1", "Reserva expirada ou liberada"));

        assertThat(pedidos.status(p.id())).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedidos.motivo(p.id())).isEqualTo("Reserva expirada ou liberada");
        assertThat(publicador.chaves()).isEmpty();
        assertThat(ingressos.listarPorPedido(p.id())).isEmpty();
    }

    @Test
    void mesmaMensagemEntregueDuasVezes_naoRepeteOEfeito() {
        Pedido p = criar("k");
        publicador.limpar();

        service.aoReservaConfirmada("evt-1", new ReservaConfirmada(p.id(), "res-1"));
        service.aoReservaConfirmada("evt-1", new ReservaConfirmada(p.id(), "res-1"));

        assertThat(publicador.chaves()).containsExactly("pedido.pago");
        assertThat(ingressos.listarPorPedido(p.id())).hasSize(3);
    }

    @Test
    void duasConfirmacoesComEventoIdsDiferentes_emitemIngressosUmaVezSo() {
        Pedido p = criar("k");
        publicador.limpar();

        service.aoReservaConfirmada("evt-1", new ReservaConfirmada(p.id(), "res-1"));
        service.aoReservaConfirmada("evt-2", new ReservaConfirmada(p.id(), "res-1"));

        assertThat(publicador.chaves()).containsExactly("pedido.pago");
        assertThat(ingressos.listarPorPedido(p.id())).hasSize(3);
    }

    @Test
    void entregasSimultaneasDaMesmaMensagem_aplicamATransicaoUmaVezSo() throws Exception {
        Pedido p = criar("k");
        publicador.limpar();
        processados.ignorarChecagemRapida = true;

        Thread a = new Thread(() -> service.aoReservaConfirmada("evt-1", new ReservaConfirmada(p.id(), "res-1")));
        Thread b = new Thread(() -> service.aoReservaConfirmada("evt-1", new ReservaConfirmada(p.id(), "res-1")));
        a.start();
        b.start();
        a.join();
        b.join();

        assertThat(publicador.chaves()).containsExactly("pedido.pago");
    }

    @Test
    void mensagemAtrasadaDeUmPedidoJaFinal_naoMudaNada() {
        Pedido p = criar("k");
        service.aoReservaFalhou("evt-2", new ReservaConfirmacaoFalhou(p.id(), "res-1", "expirou"));
        publicador.limpar();

        service.aoReservaConfirmada("evt-3", new ReservaConfirmada(p.id(), "res-1"));
        service.aoReservaFalhou("evt-4", new ReservaConfirmacaoFalhou(p.id(), "res-1", "repetida"));

        assertThat(pedidos.status(p.id())).isEqualTo(StatusPedido.CANCELADO);
        assertThat(ingressos.listarPorPedido(p.id())).isEmpty();
        assertThat(publicador.chaves()).isEmpty();
    }

    @Test
    void mensagemSobrePedidoInexistente_eIgnorada() {
        service.aoReservaFalhou("evt-1", new ReservaConfirmacaoFalhou("nao-existe", "res", "x"));
        service.aoReservaConfirmada("evt-2", new ReservaConfirmada("nao-existe", "res"));

        assertThat(publicador.chaves()).isEmpty();
    }

    @Test
    void soODonoConsultaOPedido_osOutrosRecebem404() {
        Pedido p = criar("k");

        assertThat(service.consultar("ana@x.com", p.id()).pedido().id()).isEqualTo(p.id());
        assertThatThrownBy(() -> service.consultar("bia@x.com", p.id()))
                .isInstanceOf(NaoEncontradoException.class).hasMessage("Pedido não encontrado");
        assertThatThrownBy(() -> service.consultar("ana@x.com", "nao-existe"))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void ingressosSoVemQuandoOPedidoEstaPago() {
        Pedido p = criar("k");
        assertThat(service.consultar("ana@x.com", p.id()).ingressos()).isEmpty();

        service.aoReservaConfirmada("evt-2", new ReservaConfirmada(p.id(), "res-1"));
        assertThat(service.consultar("ana@x.com", p.id()).ingressos()).hasSize(3);
    }

    @Test
    void meusIngressosSoMostraOsDoUsuario() {
        Pedido ana = criar("k");
        service.aoReservaConfirmada("evt-2", new ReservaConfirmada(ana.id(), "res-1"));

        assertThat(service.meusIngressos("ana@x.com")).hasSize(3);
        assertThat(service.meusIngressos("bia@x.com")).isEmpty();
        assertThat(service.meusPedidos("ana@x.com")).hasSize(1);
        assertThat(service.meusPedidos("bia@x.com")).isEmpty();
    }

    private static Sacola sacola(Instant expiraEm) {
        return new Sacola("res-1", "ses-1", expiraEm, List.of(
                new ItemPedido("st-pista", "Pista", 2, new BigDecimal("120.00"), new BigDecimal("240.00")),
                new ItemPedido("st-camarote", "Camarote", 1, new BigDecimal("300.00"), new BigDecimal("300.00"))),
                new BigDecimal("540.00"));
    }

    static class SemTransacao implements UnidadeDeTrabalho {
        @Override
        public <T> T executar(Supplier<T> trabalho) {
            return trabalho.get();
        }
    }

    static class FakeFila implements FilaClient {
        Sacola sacola = sacola(AGORA.plusSeconds(300));
        boolean indisponivel = false;
        volatile String ultimaCredencial;

        @Override
        public Sacola buscarSacola(String credencial, String sessaoId) {
            ultimaCredencial = credencial;
            if (indisponivel) throw new ServicoIndisponivelException("fila fora do ar");
            if (sacola == null) throw new NaoEncontradoException("Sacola vazia");
            return sacola;
        }
    }

    static class FakeEstoque implements EstoqueClient {
        @Override
        public InfoSessao buscarSessao(String sessaoId) {
            return new InfoSessao("evt-1", "Festival Aurora", LocalDateTime.of(2027, 10, 18, 14, 0));
        }
    }

    static class FakePedidos implements PedidoRepository {
        private final Map<String, Pedido> porId = new LinkedHashMap<>();

        synchronized int total() {
            return porId.size();
        }

        synchronized StatusPedido status(String id) {
            return porId.get(id).status();
        }

        synchronized String motivo(String id) {
            return porId.get(id).motivo();
        }

        private static boolean usaReserva(Pedido p) {
            return p.status() == StatusPedido.PENDENTE || p.status() == StatusPedido.CONFIRMANDO
                    || p.status() == StatusPedido.PAGO;
        }

        @Override
        public synchronized void inserir(Pedido novo) {
            boolean chaveRepetida = porId.values().stream().anyMatch(p ->
                    p.usuarioId().equals(novo.usuarioId()) && p.chaveIdempotencia().equals(novo.chaveIdempotencia()));
            boolean reservaEmUso = porId.values().stream().anyMatch(p ->
                    p.reservaId().equals(novo.reservaId()) && usaReserva(p));
            if (chaveRepetida || reservaEmUso) throw new DuplicadoException("chave única violada");
            porId.put(novo.id(), novo);
        }

        @Override
        public synchronized Optional<Pedido> buscarPorId(String id) {
            return Optional.ofNullable(porId.get(id));
        }

        // Simula a janela da corrida: a primeira busca por chave "não vê" o pedido que já foi gravado
        volatile boolean naoEnxergarChavePorEnquanto = false;

        @Override
        public synchronized Optional<Pedido> buscarPorUsuarioEChave(String usuarioId, String chave) {
            if (naoEnxergarChavePorEnquanto) {
                naoEnxergarChavePorEnquanto = false;
                return Optional.empty();
            }
            return porId.values().stream().filter(p ->
                    p.usuarioId().equals(usuarioId) && p.chaveIdempotencia().equals(chave)).findFirst();
        }

        @Override
        public synchronized Optional<Pedido> buscarAtivoPorReservaId(String reservaId) {
            return porId.values().stream().filter(p -> p.reservaId().equals(reservaId) && usaReserva(p)).findFirst();
        }

        @Override
        public synchronized List<Pedido> listarPorUsuario(String usuarioId) {
            return porId.values().stream().filter(p -> p.usuarioId().equals(usuarioId))
                    .sorted(Comparator.comparing(Pedido::criadoEm).reversed()).toList();
        }

        @Override
        public synchronized List<Pedido> listarTodos() {
            return List.copyOf(porId.values());
        }

        @Override
        public synchronized boolean transicionar(String id, StatusPedido de, StatusPedido para, String motivo, Instant agora) {
            Pedido p = porId.get(id);
            if (p == null || p.status() != de) return false;
            porId.put(id, new Pedido(p.id(), p.usuarioId(), p.chaveIdempotencia(), p.sessaoId(), p.eventoId(),
                    p.eventoTitulo(), p.dataEvento(), p.reservaId(), p.itens(), p.total(), para, motivo,
                    p.criadoEm(), agora));
            return true;
        }
    }

    static class FakeIngressos implements IngressoRepository {
        private final List<IngressoEmitido> todos = new ArrayList<>();

        @Override
        public synchronized void salvarTodos(List<IngressoEmitido> novos) {
            todos.addAll(novos);
        }

        @Override
        public synchronized List<IngressoEmitido> listarPorPedido(String pedidoId) {
            return todos.stream().filter(i -> i.pedidoId().equals(pedidoId)).toList();
        }

        @Override
        public synchronized List<IngressoEmitido> listarPorUsuario(String usuarioId) {
            return todos.stream().filter(i -> i.usuarioId().equals(usuarioId)).toList();
        }
    }

    static class FakePublicador implements PublicadorDeEventos {
        private final List<Object[]> mensagens = new ArrayList<>();

        @Override
        public synchronized void publicar(String routingKey, Object dados) {
            mensagens.add(new Object[] { routingKey, dados });
        }

        synchronized List<String> chaves() {
            return mensagens.stream().map(m -> (String) m[0]).toList();
        }

        synchronized Object dados(int indice) {
            return mensagens.get(indice)[1];
        }

        synchronized void limpar() {
            mensagens.clear();
        }
    }

    static class FakeProcessados implements EventosProcessados {
        private final Set<String> ids = new HashSet<>();
        volatile boolean ignorarChecagemRapida = false;

        @Override
        public synchronized boolean jaProcessado(String eventoId) {
            return !ignorarChecagemRapida && ids.contains(eventoId);
        }

        @Override
        public synchronized void registrar(String eventoId) {
            if (!ids.add(eventoId)) throw new MensagemDuplicadaException(eventoId);
        }
    }
}
