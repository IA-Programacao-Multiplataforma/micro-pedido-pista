# Pedido (porta 8083)

**Onde começa o ciclo de venda.** Cria o pedido, avisa o estoque por mensagem, atualiza o status com a resposta, emite os ingressos e gera o relatório de vendas. Spring Boot, MongoDB (banco `pm_pedido`), RabbitMQ, validação de JWT.

## Estados do pedido
```
PROCESSANDO ──(estoque: pedido.realizado)──> REALIZADO   (ingressos emitidos)
PROCESSANDO ──(estoque: pedido.cancelado)──> CANCELADO   (com o motivo)
```

## Mensageria
| | |
|---|---|
| **Envia** | `pedido.criado` → estoque, logo depois de gravar o pedido `PROCESSANDO` |
| **Recebe** | `pedido.realizado` / `pedido.cancelado` (fila `pedido.resposta-estoque`) |

O [`PedidoService`](src/main/java/com/pista/pedido/domain/service/PedidoService.java) só troca o status se o pedido ainda estiver `PROCESSANDO`. Por isso, uma mensagem repetida não emite ingressos duas vezes.

## Endpoints
Pelo gateway o prefixo é `/pedido`. Todos exigem token.

| Método e caminho | O que faz | Acesso |
|---|---|---|
| `POST /pedidos` | **Início do ciclo**: cria o pedido `PROCESSANDO` e responde **202** | logado |
| `GET /pedidos/{id}` | Status do pedido (o app consulta a cada 2 s) | dono do pedido |
| `GET /pedidos/meus` | Meus pedidos | logado |
| `GET /ingressos/meus` | Meus ingressos | logado |
| `GET /relatorios/vendas` | Vendas por evento e setor (só pedidos `REALIZADO`) | **ADMIN** |

`POST /pedidos` recebe o header **`Idempotency-Key`** e o corpo:
```json
{ "sessaoId": "...", "itens": [ { "setorId": "...", "quantidade": 2 } ] }
```
Repetir a mesma chave devolve o mesmo pedido, sem criar outro (protege contra duplo toque). Não há pagamento.

## Configuração
| Variável | Para quê |
|---|---|
| `MONGO_URI` | MongoDB Atlas |
| `RABBITMQ_URI` | CloudAMQP |
| `JWT_SECRET` | segredo do JWT (o mesmo do login e do gateway) |

## Como rodar e testar
```bash
./mvnw spring-boot:run
./mvnw test      # testes de domínio (PedidoServiceTest, RelatorioServiceTest), sem banco nem RabbitMQ
```
Para testar sem mexer nos dados reais, defina `PEDIDO_MONGO_DATABASE` com outro nome de banco.
