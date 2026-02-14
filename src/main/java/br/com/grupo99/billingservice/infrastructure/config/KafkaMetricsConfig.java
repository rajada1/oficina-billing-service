package br.com.grupo99.billingservice.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de métricas Kafka para o Billing Service.
 * Integra com Prometheus/Micrometer para dashboards New Relic/Grafana.
 * 
 * Métricas expostas:
 * - kafka.publisher.events.total: Total de eventos publicados por tipo
 * - kafka.publisher.events.failed: Total de falhas por tipo de evento
 * - kafka.publisher.latency: Latência de publicação
 * - kafka.circuitbreaker.state: Estado do Circuit Breaker
 */
@Slf4j
@Configuration
public class KafkaMetricsConfig {

    private final MeterRegistry meterRegistry;

    public KafkaMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("📊 Kafka Metrics configurado para monitoramento - Billing Service");
    }

    // ===================== CONTADORES DE EVENTOS =====================

    @Bean
    public Counter billingEventosPublicados() {
        return Counter.builder("kafka.publisher.events.total")
                .description("Total de eventos Billing publicados com sucesso")
                .tag("service", "billing-service")
                .tag("topic", KafkaConfig.TOPIC_BILLING_EVENTS)
                .tag("status", "success")
                .register(meterRegistry);
    }

    @Bean
    public Counter billingEventosFalhos() {
        return Counter.builder("kafka.publisher.events.failed")
                .description("Total de eventos Billing que falharam na publicação")
                .tag("service", "billing-service")
                .tag("topic", KafkaConfig.TOPIC_BILLING_EVENTS)
                .tag("status", "failed")
                .register(meterRegistry);
    }

    @Bean
    public Counter billingDltEventosEnviados() {
        return Counter.builder("kafka.dlt.events.total")
                .description("Total de eventos enviados para Dead Letter Topics")
                .tag("service", "billing-service")
                .tag("topic", "dlt")
                .register(meterRegistry);
    }

    // ===================== CONTADORES POR TIPO DE EVENTO =====================

    @Bean
    public Counter orcamentoProntoCounter() {
        return Counter.builder("kafka.publisher.events.by_type")
                .description("Eventos por tipo")
                .tag("service", "billing-service")
                .tag("event_type", "ORCAMENTO_PRONTO")
                .register(meterRegistry);
    }

    @Bean
    public Counter orcamentoAprovadoCounter() {
        return Counter.builder("kafka.publisher.events.by_type")
                .description("Eventos por tipo")
                .tag("service", "billing-service")
                .tag("event_type", "ORCAMENTO_APROVADO")
                .tag("saga_action", "step")
                .register(meterRegistry);
    }

    @Bean
    public Counter orcamentoRejeitadoCounter() {
        return Counter.builder("kafka.publisher.events.by_type")
                .description("Eventos por tipo - Compensação")
                .tag("service", "billing-service")
                .tag("event_type", "ORCAMENTO_REJEITADO")
                .tag("saga_action", "compensation")
                .register(meterRegistry);
    }

    @Bean
    public Counter pagamentoFalhouCounter() {
        return Counter.builder("kafka.publisher.events.by_type")
                .description("Eventos por tipo - Compensação")
                .tag("service", "billing-service")
                .tag("event_type", "PAGAMENTO_FALHOU")
                .tag("saga_action", "compensation")
                .register(meterRegistry);
    }

    // ===================== TIMERS DE LATÊNCIA =====================

    @Bean
    public Timer billingKafkaPublishLatency() {
        return Timer.builder("kafka.publisher.latency")
                .description("Latência de publicação de eventos Kafka")
                .tag("service", "billing-service")
                .tag("topic", KafkaConfig.TOPIC_BILLING_EVENTS)
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    @Bean
    public Timer billingSagaLatency() {
        return Timer.builder("saga.step.latency")
                .description("Latência de cada etapa da Saga")
                .tag("service", "billing-service")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    // ===================== MÉTRICAS DE CIRCUIT BREAKER =====================

    @Bean
    public Counter billingCircuitBreakerOpenCounter() {
        return Counter.builder("kafka.circuitbreaker.opened")
                .description("Quantidade de vezes que o Circuit Breaker abriu")
                .tag("service", "billing-service")
                .tag("circuit_breaker", "kafkaPublisher")
                .register(meterRegistry);
    }

    @Bean
    public Counter billingCircuitBreakerFallbackCounter() {
        return Counter.builder("kafka.circuitbreaker.fallback")
                .description("Quantidade de chamadas ao fallback")
                .tag("service", "billing-service")
                .tag("circuit_breaker", "kafkaPublisher")
                .register(meterRegistry);
    }

    // ===================== MÉTRICAS DE CONSUMER =====================

    @Bean
    public Counter billingEventosConsumidos() {
        return Counter.builder("kafka.consumer.events.total")
                .description("Total de eventos consumidos")
                .tag("service", "billing-service")
                .tag("status", "success")
                .register(meterRegistry);
    }

    @Bean
    public Counter billingEventosConsumidosErro() {
        return Counter.builder("kafka.consumer.events.failed")
                .description("Total de eventos consumidos com erro")
                .tag("service", "billing-service")
                .tag("status", "failed")
                .register(meterRegistry);
    }

    @Bean
    public Timer billingKafkaConsumeLatency() {
        return Timer.builder("kafka.consumer.latency")
                .description("Latência de processamento de eventos consumidos")
                .tag("service", "billing-service")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }
}
