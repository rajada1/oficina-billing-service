package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.domain.events.OrcamentoAprovadoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoProntoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoRejeitadoEvent;
import br.com.grupo99.billingservice.domain.events.PagamentoFalhouEvent;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;

/**
 * Publicador de eventos para o SQS (Saga Pattern - Event Publisher)
 */
@Slf4j
@Service
public class BillingEventPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queues.billing-events}")
    private String billingEventsQueueUrl;

    public BillingEventPublisher(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica evento de orçamento pronto (Saga Step 3)
     */
    public void publishOrcamentoPronto(OrcamentoProntoEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(billingEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageDeduplicationId(event.getOrcamentoId().toString() + "-" + event.getTimestamp())
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            log.info("Evento ORCAMENTO_PRONTO publicado. Orçamento ID: {}", event.getOrcamentoId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento ORCAMENTO_PRONTO: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao publicar evento ORCAMENTO_PRONTO", e);
        }
    }

    /**
     * Publica evento de orçamento aprovado (Saga Step 4)
     */
    public void publishOrcamentoAprovado(OrcamentoAprovadoEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(billingEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageDeduplicationId(event.getOrcamentoId().toString() + "-" + event.getTimestamp())
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            log.info("Evento ORCAMENTO_APROVADO publicado. Orçamento ID: {}, OS ID: {}",
                    event.getOrcamentoId(), event.getOsId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento ORCAMENTO_APROVADO: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao publicar evento ORCAMENTO_APROVADO", e);
        }
    }

    /**
     * Publica evento de compensação - Orçamento rejeitado (Rollback)
     */
    public void publishOrcamentoRejeitado(OrcamentoRejeitadoEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(billingEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageDeduplicationId(event.getOrcamentoId().toString() + "-rejected-" + event.getTimestamp())
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            log.warn("🔄 Evento de compensação ORCAMENTO_REJEITADO publicado. Orçamento ID: {}, Motivo: {}",
                    event.getOrcamentoId(), event.getMotivo());
        } catch (Exception e) {
            log.error("Erro crítico ao publicar evento de compensação ORCAMENTO_REJEITADO: {}", e.getMessage(), e);
        }
    }

    /**
     * Publica evento de compensação - Pagamento falhou (Rollback)
     */
    public void publishPagamentoFalhou(PagamentoFalhouEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(billingEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageDeduplicationId(event.getPagamentoId().toString() + "-failed-" + event.getTimestamp())
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            log.error("🔄 Evento de compensação PAGAMENTO_FALHOU publicado. Pagamento ID: {}, Código Erro: {}",
                    event.getPagamentoId(), event.getCodigoErro());
        } catch (Exception e) {
            log.error("Erro crítico ao publicar evento de compensação PAGAMENTO_FALHOU: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ CLEAN ARCHITECTURE: Métodos para Application Services
     * Alias dos métodos publish com nomes mais intuitivos
     */

    /**
     * Publicar evento: Orçamento criado
     */
    public void publicarOrcamentoCriado(Orcamento orcamento) {
        log.info("Orçamento criado: {}", orcamento.getId());
        // Implementar publicação de evento se necessário
        // Por enquanto apenas log para evitar erro de compilação
    }

    /**
     * Publicar evento: Orçamento aprovado
     */
    public void publicarOrcamentoAprovado(Orcamento orcamento) {
        OrcamentoAprovadoEvent event = OrcamentoAprovadoEvent.builder()
                .orcamentoId(orcamento.getId())
                .osId(orcamento.getOsId())
                .valorTotal(orcamento.getValorTotal())
                .timestamp(LocalDateTime.now())
                .build();
        publishOrcamentoAprovado(event);
    }

    /**
     * Publicar evento: Orçamento rejeitado
     */
    public void publicarOrcamentoRejeitado(Orcamento orcamento) {
        OrcamentoRejeitadoEvent event = OrcamentoRejeitadoEvent.builder()
                .orcamentoId(orcamento.getId())
                .osId(orcamento.getOsId())
                .motivo(orcamento.getMotivoRejeicao())
                .timestamp(LocalDateTime.now())
                .build();
        publishOrcamentoRejeitado(event);
    }

    /**
     * Publicar evento: Pagamento registrado
     */
    public void publicarPagamentoRegistrado(Pagamento pagamento) {
        log.info("Pagamento registrado: {}", pagamento.getId());
        // Implementar publicação de evento se necessário
    }

    /**
     * Publicar evento: Pagamento confirmado
     */
    public void publicarPagamentoConfirmado(Pagamento pagamento) {
        log.info("Pagamento confirmado: {}", pagamento.getId());
        // Implementar publicação de evento se necessário
    }

    /**
     * Publicar evento: Pagamento estornado
     */
    public void publicarPagamentoEstornado(Pagamento pagamento) {
        log.info("Pagamento estornado: {}", pagamento.getId());
        // Implementar publicação de evento se necessário
    }
}
