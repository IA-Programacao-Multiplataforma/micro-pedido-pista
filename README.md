# Pedido (porta 8083)

Cria o pedido, **orquestra a compra por mensagens**, emite os ingressos e gera o relatório de vendas. Spring Boot, MongoDB (banco `pm_pedido`), RabbitMQ, validação de JWT.

## Endpoints
Pelo gateway o prefixo é `/pedido`. Todos exigem token.

| Método e caminho | O que faz | Acesso |
|---|---|---|
| `POST /pedidos` | Cria o pedido a partir da sacola e responde **202** na hora | logado |
| `GET /pedidos/{id}` | Estado do pedido (o app consulta a cada 2 s) | dono do pedido |
| `GET /pedidos/meus` | Meus pedidos | logado |
| `GET /ingressos/meus` | Meus ingressos (evento, data, setor, código) | logado |
| `GET /relatorios/vendas` | Vendas por evento e por setor, só pedidos pagos | **ADMIN** |

`POST /pedidos` confirma a compra da sacola. Exige o header **`Idempotency-Key`** e o corpo `{ "sessaoId" }`. **Não há pagamento:** todo pedido confirmado é aprovado. Repetir a mesma chave devolve o mesmo pedido, sem criar outro.

## Estados do pedido
`CONFIRMANDO` → `PAGO` | `CONFIRMANDO` → `CANCELADO` (a reserva expirou antes da confirmação).

## A saga (RabbitMQ)
| Passo | Mensagem |
|---|---|
| Pedido criado | publica `reserva.confirmar` → estoque |
| Resultado da reserva | consome `reserva.confirmada` / `reserva.confirmacao-falhou` (fila `pedido.reserva-resultado`) |
| Confirmada | emite os ingressos e publica `pedido.pago` → fila |
| Reserva não confirmada | cancela o pedido |

Garantias: **Transactional Outbox** (a mensagem é gravada na mesma transação da mudança de estado e publicada depois), **consumidores idempotentes** (tabela de mensagens processadas) e troca de estado condicional (`PENDENTE`→`CONFIRMANDO` só uma vez, mesmo com duas mensagens simultâneas). Falhou 3 vezes, vai para a fila morta (`.dlq`).

## Regras de negócio
- A sacola precisa ser do usuário e ter pelo menos `pedido.tempo-minimo-pagar-segundos` (60 s) de vida.
- Uma sacola só vira um pedido ativo por vez (índice único); depois de uma recusa dá para tentar outro cartão.
- O relatório conta como venda só `PAGO`; recusados, cancelados e em andamento aparecem como contagem.

## Configuração
| Variável | Para quê |
|---|---|
| `MONGO_URI` | MongoDB Atlas |
| `RABBITMQ_URI` | CloudAMQP |
| `JWT_SECRET` | segredo do JWT (o mesmo do login e do gateway) |

Consulta a fila e o estoque por REST (`fila.url`, `estoque.url`), só leitura.

## Como rodar e testar
```bash
./mvnw spring-boot:run
./mvnw test      # 30 testes, sem banco nem RabbitMQ
```
Para testar sem mexer nos dados reais, defina `PEDIDO_MONGO_DATABASE` com outro nome de banco.
