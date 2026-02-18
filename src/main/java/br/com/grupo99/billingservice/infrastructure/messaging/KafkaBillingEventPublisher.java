package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.domain.events.OrcamentoAprovadoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoProntoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoRejeitadoEvent;
import br.com.grupo99.billingservice.domain.events.PagamentoFalhouEvent;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.infrastructure.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Publicador de eventos Kafka para o Billing Service
 * Implementa o padrão Saga Coreografada para eventos de orçamento e pagamento.
 * 
 * Tópico: billing-events
 * Partition Key: osId (garante ordenação por OS)
 * 
 * Resiliência:
 * - Circuit Breaker para proteção contra falhas do broker
 * - Retry com backoff exponencial
 */
@Slf4j
@Service
@Primary
public class KafkaBillingEventPublisher implements BillingEventPublisherPort {

    private static final String CIRCUIT_BREAKER_NAME = "kafkaPublisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaBillingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "publishOrcamentoProntoFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void publishOrcamentoPronto(OrcamentoProntoEvent event) {
        String key = event.getOsId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_BILLING_EVENTS, key, event);

        record.headers()
                .add(new RecordHeader("eventType", "ORCAMENTO_PRONTO".getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("osId", key.getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("orcamentoId",
                        event.getOrcamentoId().toString().getBytes(StandardCharsets.UTF_8)));

        sendAsync(record, "ORCAMENTO_PRONTO", event.getOrcamentoId().toString());
    }

    public void publishOrcamentoProntoFallback(OrcamentoProntoEvent event, Throwable t) {
        log.error("🔴 Circuit Breaker ABERTO - Evento ORCAMENTO_PRONTO não publicado. OS ID: {}, Erro: {}",
                event.getOsId(), t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "publishOrcamentoAprovadoFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void publishOrcamentoAprovado(OrcamentoAprovadoEvent event) {
        String key = event.getOsId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_BILLING_EVENTS, key, event);

        record.headers()
                .add(new RecordHeader("eventType", "ORCAMENTO_APROVADO".getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("osId", key.getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("orcamentoId",
                        event.getOrcamentoId().toString().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("valorTotal",
                        String.valueOf(event.getValorTotal()).getBytes(StandardCharsets.UTF_8)));

        // Evento crítico - envio síncrono
        sendSync(record, "ORCAMENTO_APROVADO", event.getOrcamentoId().toString());
    }

    public void publishOrcamentoAprovadoFallback(OrcamentoAprovadoEvent event, Throwable t) {
        log.error("🔴 Circuit Breaker ABERTO - Evento CRÍTICO ORCAMENTO_APROVADO não publicado. OS ID: {}, Erro: {}",
                event.getOsId(), t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "publishOrcamentoRejeitadoFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void publishOrcamentoRejeitado(OrcamentoRejeitadoEvent event) {
        String key = event.getOsId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_BILLING_EVENTS, key, event);

        record.headers()
                .add(new RecordHeader("eventType", "ORCAMENTO_REJEITADO".getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("osId", key.getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("motivo", event.getMotivo().getBytes(StandardCharsets.UTF_8)));

        // Evento de compensação - envio síncrono
        sendSync(record, "ORCAMENTO_REJEITADO", event.getOrcamentoId().toString());
    }

    public void publishOrcamentoRejeitadoFallback(OrcamentoRejeitadoEvent event, Throwable t) {
        log.error(
                "🔴 Circuit Breaker ABERTO - Evento de compensação ORCAMENTO_REJEITADO não publicado. OS ID: {}, Erro: {}",
                event.getOsId(), t.getMessage());
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "publishPagamentoFalhouFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void publishPagamentoFalhou(PagamentoFalhouEvent event) {
        String key = event.getOsId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_BILLING_EVENTS, key, event);

        record.headers()
                .add(new RecordHeader("eventType", "PAGAMENTO_FALHOU".getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("osId", key.getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("codigoErro", event.getCodigoErro().getBytes(StandardCharsets.UTF_8)));

        // Evento de compensação - envio síncrono
        sendSync(record, "PAGAMENTO_FALHOU", event.getPagamentoId().toString());
    }

    public void publishPagamentoFalhouFallback(PagamentoFalhouEvent event, Throwable t) {
        log.error("🔴 Circuit Breaker ABERTO - Evento CRÍTICO PAGAMENTO_FALHOU não publicado. OS ID: {}, Erro: {}",
                event.getOsId(), t.getMessage());
    }

    // ===================== MÉTODOS DE CONVENIÊNCIA =====================

    @Override
    public void publicarOrcamentoCriado(Orcamento orcamento) {
        OrcamentoProntoEvent event = OrcamentoProntoEvent.builder()
                .orcamentoId(orcamento.getId())
                .osId(orcamento.getOsId())
                .valorTotal(orcamento.getValorTotal())
                .timestamp(LocalDateTime.now())
                .eventType("ORCAMENTO_CRIADO")
                .build();

        publishOrcamentoPronto(event);
    }

    @Override
    public void publicarOrcamentoAprovado(Orcamento orcamento) {
        OrcamentoAprovadoEvent event = OrcamentoAprovadoEvent.builder()
                .orcamentoId(orcamento.getId())
                .osId(orcamento.getOsId())
                .valorTotal(orcamento.getValorTotal())
                .timestamp(LocalDateTime.now())
                .eventType("ORCAMENTO_APROVADO")
                .build();

        publishOrcamentoAprovado(event);
    }

    @Override
    public void publicarOrcamentoRejeitado(Orcamento orcamento) {
        OrcamentoRejeitadoEvent event = OrcamentoRejeitadoEvent.builder()
                .orcamentoId(orcamento.getId())
                .osId(orcamento.getOsId())
                .motivo(orcamento.getMotivoRejeicao())
                .timestamp(LocalDateTime.now())
                .eventType("ORCAMENTO_REJEITADO")
                .build();

        publishOrcamentoRejeitado(event);
    }

    @Override
    public void publicarPagamentoRegistrado(Pagamento pagamento) {
        log.info("📝 Pagamento registrado. ID: {}, OS: {}, Valor: R$ {}",
                pagamento.getId(), pagamento.getOsId(), pagamento.getValor());
        // Evento interno - não propaga para Saga
    }

    @Override
    public void publicarPagamentoConfirmado(Pagamento pagamento) {
        log.info("✅ Pagamento confirmado. ID: {}, OS: {}, Valor: R$ {}",
                pagamento.getId(), pagamento.getOsId(), pagamento.getValor());
        // Pode disparar evento de pagamento confirmado se necessário
    }

    @Override
    public void publicarPagamentoEstornado(Pagamento pagamento) {
        PagamentoFalhouEvent event = PagamentoFalhouEvent.builder()
                .pagamentoId(pagamento.getId())
                .osId(pagamento.getOsId())
                .orcamentoId(pagamento.getOrcamentoId())
                .codigoErro("ESTORNO")
                .mensagemErro("Pagamento estornado")
                .timestamp(LocalDateTime.now())
                .eventType("PAGAMENTO_ESTORNADO")
                .build();

        publishPagamentoFalhou(event);
    }

    // ===================== MÉTODOS AUXILIARES =====================

    @SuppressWarnings("null")
    private void sendAsync(ProducerRecord<String, Object> record, String eventType, String id) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Evento {} publicado no Kafka. ID: {}, Topic: {}, Partition: {}, Offset: {}",
                        eventType, id,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Erro ao publicar evento {}: {}", eventType, ex.getMessage(), ex);
            }
        });
    }

    @SuppressWarnings("null")
    private void sendSync(ProducerRecord<String, Object> record, String eventType, String id) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(record).get();
            log.info("✅ Evento {} publicado (síncrono). ID: {}, Partition: {}, Offset: {}",
                    eventType, id,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO ao publicar evento {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Falha ao publicar evento " + eventType, e);
        }
    }
}
