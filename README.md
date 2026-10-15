# Ticket Reservation System

Sistema de reservas de ingressos para eventos que demonstra, lado a lado, as duas
técnicas clássicas de controle de concorrência em bancos relacionais: **lock
otimista** (coluna `@Version` + retry) e **lock pessimista** (`SELECT ... FOR
UPDATE`). O objetivo é provar — com um teste de carga concorrente real, não só
no papel — que nenhuma das duas estratégias vende mais ingressos do que existem.

## Stack

- Java 21, Spring Boot 4
- Spring Data JPA (Hibernate) + PostgreSQL
- Flyway para migrations versionadas
- Bean Validation
- Testcontainers (testes de integração contra um Postgres real, não H2)
- JUnit 5 + AssertJ

## O problema de concorrência

Um `TicketType` (ex: "Pista", "Camarote") tem uma quantidade limitada de
ingressos. Quando N clientes tentam reservar ao mesmo tempo, o sistema não pode
decrementar `available_quantity` abaixo de zero nem permitir que duas
transações concorrentes leiam o mesmo valor "disponível" e ambas decrementem
com base nele (classic lost update / overselling).

Duas estratégias resolvem isso, selecionáveis por requisição via
`?strategy=optimistic|pessimistic`:

| | Lock otimista | Lock pessimista |
|---|---|---|
| Como funciona | Lê a linha, decrementa em memória, tenta commitar; se a versão mudou, falha e tenta de novo | Trava a linha com `FOR UPDATE` antes de ler; concorrentes esperam na fila |
| Custo sob baixa contenção | Baixo (sem locks de linha) | Um lock curto por transação |
| Custo sob alta contenção | Retries desperdiçados, risco de inanição | Serializa corretamente, sem trabalho desperdiçado, mas enfileira |
| Implementação | [`OptimisticReservationStrategy`](src/main/java/com/portfolio/ticketreservation/service/OptimisticReservationStrategy.java) + [`TicketTypeOptimisticMutator`](src/main/java/com/portfolio/ticketreservation/service/TicketTypeOptimisticMutator.java) | [`PessimisticReservationStrategy`](src/main/java/com/portfolio/ticketreservation/service/PessimisticReservationStrategy.java) |

A prova está em
[`ReservationConcurrencyTest`](src/test/java/com/portfolio/ticketreservation/service/ReservationConcurrencyTest.java):
60 threads disputam 15 assentos, para cada estratégia, e o teste garante que
exatamente 15 reservas sejam bem-sucedidas, as outras 45 falhem de forma
previsível, e o contador final seja exatamente zero — nunca negativo, nunca
"vazou" disponibilidade.

### Uma pegadinha de proxy do Spring, de propósito

`attemptReserve`, `confirm` e `releaseOne` moram em beans **separados**
(`TicketTypeOptimisticMutator`, `PaymentConfirmer`, `ReservationReleaser`) em
vez de serem métodos privados/auto-invocados nas classes que os chamam. Isso
não é estilo: é necessário. O proxy do Spring que aplica `@Transactional` só
intercepta chamadas que passam *pelo proxy* — uma chamada `this.metodo()` dentro
da mesma classe nunca passa pelo proxy, então a anotação seria silenciosamente
ignorada. Separar em beans é a forma padrão (e documentada) de evitar essa
armadilha.

## Idempotência

Criar uma reserva exige o header `Idempotency-Key`; confirmar um pagamento
também. Replays da mesma chave — sequenciais (retry de cliente após timeout) ou
concorrentes (duas cópias da mesma requisição em voo) — devem devolver o
**mesmo** recurso, nunca duplicar a reserva ou cobrar duas vezes. Isso é
garantido em duas camadas:

1. Antes de qualquer trabalho, procura por uma linha existente com aquela chave.
2. Se duas requisições passarem pela checagem ao mesmo tempo, a constraint
   `UNIQUE (idempotency_key)` no banco garante que só uma `INSERT` vence; a
   perdedora recebe `DataIntegrityViolationException`, que o serviço
   intercepta para buscar e devolver o registro que já existe.

Veja [`ReservationIdempotencyTest`](src/test/java/com/portfolio/ticketreservation/service/ReservationIdempotencyTest.java).

## Expiração de reservas

Uma reserva fica `PENDING` por `reservation.hold-duration` (padrão 15 min). Se
não for confirmada, [`ReservationExpirationScheduler`](src/main/java/com/portfolio/ticketreservation/service/ReservationExpirationScheduler.java)
a marca como `EXPIRED` e devolve os assentos ao `TicketType`, usando o mesmo
lock pessimista da estratégia `pessimistic` para fazer isso com segurança ao
lado de reservas concorrentes em andamento.

## Rodando localmente

```bash
docker compose up -d        # Postgres em localhost:5432
./mvnw spring-boot:run
```

### Fluxo de exemplo

```bash
# 1. Criar um evento
curl -X POST localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Show X","venue":"Arena Y","eventDate":"2027-01-01T20:00:00"}'

# 2. Criar um tipo de ingresso (eventId=1, 100 unidades)
curl -X POST localhost:8080/api/events/1/ticket-types \
  -H "Content-Type: application/json" \
  -d '{"name":"Pista","priceCents":15000,"totalQuantity":100}'

# 3. Reservar (strategy=optimistic é o padrão)
curl -X POST "localhost:8080/api/ticket-types/1/reservations?strategy=pessimistic" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pedido-abc-123" \
  -d '{"customerId":"cliente-42","quantity":2}'

# 4. Confirmar pagamento (reservationId=1)
curl -X POST localhost:8080/api/reservations/1/payments \
  -H "Idempotency-Key: pagamento-abc-123"
```

## Testes

```bash
./mvnw test
```

Os testes de integração sobem um Postgres real via Testcontainers (requer
Docker rodando) — não usam H2, para que o comportamento de locking testado seja
o mesmo do banco de produção.

## O que este projeto demonstra

- Controle de concorrência real (não um `synchronized` de brinquedo): duas
  estratégias de banco, comparadas, com teste de carga provando ausência de
  overselling.
- Idempotência correta sob concorrência, não só "checar antes de inserir".
- Entendimento de limites do proxy do Spring (`@Transactional` e
  auto-invocação) e como projetar em torno disso.
- Migrations versionadas (Flyway) com constraints de banco fazendo parte da
  defesa (`CHECK`, `UNIQUE`), não só validação na aplicação.
- Separação de liberação de recursos (expiração) usando a mesma primitiva de
  lock que a reserva, para não reabrir a race que o lock pessimista evita.
