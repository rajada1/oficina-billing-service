package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.service.OrcamentoApplicationService;
import br.com.grupo99.billingservice.domain.events.DiagnosticoConcluidoEvent;
import br.com.grupo99.billingservice.domain.events.OSCriadaEvent;
import br.com.grupo99.billingservice.infrastructure.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Consumidor de eventos Kafka para o Billing Service
 * 
 * Consome:
 * - os-events: OS_CRIADA → Cria orçamento vazio
 * - execution-events: DIAGNOSTICO_CONCLUIDO → Calcula orçamento
 * 
 * Padrão: Saga Coreografada com Manual Acknowledgment
 */
@Slf4j
@Service
public class KafkaBillingEventListener {

    private final OrcamentoApplicationService orcamentoService;
    private final ObjectMapper objectMapper;

    public KafkaBillingEventListener(
            OrcamentoApplicationService orcamentoService,
            ObjectMapper objectMapper) {
        this.orcamentoService = orcamentoService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consome eventos do tópico os-events
     * Saga Step 2: OS_CRIADA → Criar orçamento vazio
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_OS_EVENTS, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory", concurrency = "3")
    public void consumeOSEvents(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        String eventType = extractHeader(record, "eventType");
        String osId = record.key();

        log.info("📥 Recebido evento Kafka do os-service. " +
                "Type: {}, OS ID: {}, Partition: {}, Offset: {}",
                eventType, osId, partition, offset);

        try {
            switch (eventType) {
                case "OS_CRIADA" -> handleOSCriada(record);
                case "STATUS_MUDADO" -> log.debug("Evento STATUS_MUDADO ignorado pelo billing-service");
                case "OS_CANCELADA" -> handleOSCancelada(record);
                default -> log.warn("⚠️ Tipo de evento desconhecido do os-events: {}", eventType);
            }

            acknowledgment.acknowledge();
            log.debug("✅ Evento {} commitado. Offset: {}", eventType, offset);

        } catch (Exception e) {
            log.error("❌ Erro ao processar evento do os-events. Type: {}, OS ID: {}, Erro: {}",
                    eventType, osId, e.getMessage(), e);
            handleProcessingError(record, e, "os-events");
        }
    }

    /**
     * Consome eventos do tópico execution-events
     * Saga Step 3: DIAGNOSTICO_CONCLUIDO → Calcular orçamento
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_EXECUTION_EVENTS, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory", concurrency = "2")
    public void consumeExecutionEvents(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        String eventType = extractHeader(record, "eventType");
        String osId = record.key();

        log.info("📥 Recebido evento Kafka do execution-service. " +
                "Type: {}, OS ID: {}, Partition: {}, Offset: {}",
                eventType, osId, partition, offset);

        try {
            switch (eventType) {
                case "DIAGNOSTICO_CONCLUIDO" -> handleDiagnosticoConcluido(record);
                case "EXECUCAO_CONCLUIDA" -> log.info("Execução concluída para OS: {}", osId);
                case "EXECUCAO_FALHOU" -> handleExecucaoFalhou(record);
                default -> log.warn("⚠️ Tipo de evento desconhecido do execution-events: {}", eventType);
            }

            acknowledgment.acknowledge();
            log.debug("✅ Evento {} commitado. Offset: {}", eventType, offset);

        } catch (Exception e) {
            log.error("❌ Erro ao processar evento do execution-events. Type: {}, OS ID: {}, Erro: {}",
                    eventType, osId, e.getMessage(), e);
            handleProcessingError(record, e, "execution-events");
        }
    }

    /**
     * Saga Step 2: OS criada → Criar orçamento vazio
     */
    @SuppressWarnings("unchecked")
    private void handleOSCriada(ConsumerRecord<String, Object> record) {
        try {
            UUID osId = UUID.fromString(record.key());
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String descricao = (String) payload.getOrDefault("descricao", "");

            log.info("📋 Processando OS_CRIADA. OS ID: {}, Descrição: {}", osId, descricao);

            // Cria orçamento via Application Service
            CreateOrcamentoRequest request = CreateOrcamentoRequest.builder()
                    .osId(osId)
                    .observacao("Orçamento gerado automaticamente via Kafka para OS")
                    .build();

            orcamentoService.criar(request);
            log.info("✅ Orçamento criado com sucesso para OS: {}", osId);

        } catch (IllegalArgumentException e) {
            log.warn("Orçamento já existe para OS. Ignorando evento duplicado.");
        } catch (Exception e) {
            log.error("❌ Erro ao criar orçamento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Saga Step 3: Diagnóstico concluído → Calcular orçamento
     */
    @SuppressWarnings("unchecked")
    private void handleDiagnosticoConcluido(ConsumerRecord<String, Object> record) {
        try {
            UUID osId = UUID.fromString(record.key());
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String diagnostico = (String) payload.getOrDefault("diagnostico", "");

            log.info("🔍 Processando DIAGNOSTICO_CONCLUIDO. OS ID: {}, Diagnóstico: {}",
                    osId, diagnostico);

            // TODO: Implementar lógica de cálculo do orçamento baseado no diagnóstico
            // orcamentoService.calcularOrcamento(osId, diagnostico);

            log.info("✅ Diagnóstico processado para OS: {}", osId);

        } catch (Exception e) {
            log.error("❌ Erro ao processar diagnóstico: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Saga Compensação: OS cancelada → Cancelar orçamento
     */
    @SuppressWarnings("unchecked")
    private void handleOSCancelada(ConsumerRecord<String, Object> record) {
        try {
            UUID osId = UUID.fromString(record.key());
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String motivo = (String) payload.getOrDefault("motivo", "OS cancelada");

            log.warn("🔄 Processando OS_CANCELADA. OS ID: {}, Motivo: {}", osId, motivo);

            // Cancela orçamento se existir
            try {
                orcamentoService.cancelarPorOs(osId, motivo);
                log.info("✅ Orçamento cancelado para OS: {}", osId);
            } catch (Exception e) {
                log.warn("Orçamento não encontrado para OS cancelada: {}", osId);
            }

        } catch (Exception e) {
            log.error("❌ Erro ao cancelar orçamento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Saga Compensação: Execução falhou → Reverter orçamento
     */
    @SuppressWarnings("unchecked")
    private void handleExecucaoFalhou(ConsumerRecord<String, Object> record) {
        try {
            UUID osId = UUID.fromString(record.key());
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String motivo = (String) payload.getOrDefault("motivo", "Falha na execução");
            Boolean requerRetrabalho = (Boolean) payload.getOrDefault("requerRetrabalho", false);

            log.error("💥 Processando EXECUCAO_FALHOU. OS ID: {}, Motivo: {}, Retrabalho: {}",
                    osId, motivo, requerRetrabalho);

            if (!requerRetrabalho) {
                // Cancela orçamento se não houver retrabalho
                try {
                    orcamentoService.cancelarPorOs(osId, "Execução falhou: " + motivo);
                } catch (Exception e) {
                    log.warn("Orçamento não encontrado para OS com execução falha: {}", osId);
                }
            }

        } catch (Exception e) {
            log.error("❌ Erro ao processar falha de execução: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String extractHeader(ConsumerRecord<String, Object> record, String headerKey) {
        var header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return "UNKNOWN";
    }

    private void handleProcessingError(ConsumerRecord<String, Object> record, Exception e, String source) {
        log.error("🔴 Erro crítico no processamento de evento do {}. " +
                "Topic: {}, Partition: {}, Offset: {}, Key: {}, Erro: {}",
                source,
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                e.getMessage());
        // TODO: Implementar envio para Dead Letter Topic
    }
}
