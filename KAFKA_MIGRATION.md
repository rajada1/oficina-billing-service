# Migração SQS → Kafka - Billing Service

## Visão Geral

Este documento descreve a migração do `oficina-billing-service` de AWS SQS para Apache Kafka como parte do padrão Saga Coreografada.

## Mudanças Realizadas

### 1. Novas Classes Kafka

#### `KafkaConfig.java`
- Configuração centralizada de Consumer/Producer
- Definição de tópicos: `os-events`, `billing-events`, `execution-events`
- Configuração de JSON serialization/deserialization

#### `BillingEventPublisherPort.java`
- Interface de abstração para publicação de eventos
- Permite trocar implementação SQS ↔ Kafka facilmente

#### `KafkaBillingEventPublisher.java` (@Primary)
- Implementação Kafka do publisher de eventos
- Publicação assíncrona e síncrona (eventos críticos)
- Headers para roteamento de eventos

#### `KafkaBillingEventListener.java`
- Consumidor de eventos dos tópicos `os-events` e `execution-events`
- Manual acknowledgment para garantia de processamento
- Handlers para eventos da Saga:
  - `OS_CRIADA` → Criar orçamento vazio
  - `DIAGNOSTICO_CONCLUIDO` → Calcular orçamento
  - `OS_CANCELADA` → Cancelar orçamento (compensação)
  - `EXECUCAO_FALHOU` → Reverter orçamento (compensação)

### 2. Eventos de Domínio Atualizados

Adicionado `@Builder` e novos campos aos eventos:
- `OrcamentoProntoEvent` - prazoValidadeDias
- `OrcamentoAprovadoEvent` - valorTotal, aprovadoPor
- `OrcamentoRejeitadoEvent` - rejeitadoPor
- `PagamentoFalhouEvent` - mensagemErro, valorTentado

### 3. Configurações

#### `application.yml`
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: billing-service-group
      auto-offset-reset: earliest
      enable-auto-commit: false
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    listener:
      ack-mode: manual
      concurrency: 3
```

#### `docker-compose.yml`
- Zookeeper (2181)
- Kafka (9092)
- Kafka UI (8180)
- kafka-init (criação de tópicos)

### 4. Dependências (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

## Fluxo da Saga Coreografada

```
┌─────────────┐     os-events      ┌─────────────────┐
│  os-service │ ──────────────────→│ billing-service │
│             │     OS_CRIADA      │                 │
└─────────────┘                    └─────────────────┘
                                           │
                                           │ billing-events
                                           │ ORCAMENTO_PRONTO
                                           ▼
┌──────────────────┐  execution-events  ┌─────────────────┐
│ execution-service│ ←─────────────────│ billing-service │
│                  │                    │                 │
└──────────────────┘                    └─────────────────┘
        │                                       ▲
        │ execution-events                      │
        │ DIAGNOSTICO_CONCLUIDO                 │
        └───────────────────────────────────────┘
```

## Tópicos Kafka

| Tópico | Partições | Retenção | Descrição |
|--------|-----------|----------|-----------|
| os-events | 6 | 30 dias | Eventos de Ordem de Serviço |
| billing-events | 3 | 30 dias | Eventos de Orçamento/Pagamento |
| execution-events | 3 | 30 dias | Eventos de Execução/Diagnóstico |

## Execução

### Iniciar Infraestrutura
```bash
docker-compose up -d
```

### Verificar Kafka UI
Acesse: http://localhost:8180

### Executar Aplicação
```bash
mvn spring-boot:run
```

## Compatibilidade

- SQS mantido com profile `sqs-legacy` para rollback
- `BillingEventPublisher` (SQS) permanece disponível
- `KafkaBillingEventPublisher` é `@Primary`

## Próximos Passos

1. ✅ Migrar os-service (concluído)
2. ✅ Migrar billing-service (concluído)
3. ⏳ Migrar execution-service (próximo)
4. ⏳ Configurar Dead Letter Topics
5. ⏳ Implementar Circuit Breaker
