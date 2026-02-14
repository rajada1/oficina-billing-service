package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.domain.events.OrcamentoAprovadoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoProntoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoRejeitadoEvent;
import br.com.grupo99.billingservice.domain.events.PagamentoFalhouEvent;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;

/**
 * Interface de abstração para publicação de eventos de billing.
 * Permite alternar entre SQS e Kafka de forma transparente.
 */
public interface BillingEventPublisherPort {

    /**
     * Publica evento de orçamento pronto (Saga Step 3)
     */
    void publishOrcamentoPronto(OrcamentoProntoEvent event);

    /**
     * Publica evento de orçamento aprovado (Saga Step 4)
     */
    void publishOrcamentoAprovado(OrcamentoAprovadoEvent event);

    /**
     * Publica evento de compensação - Orçamento rejeitado (Rollback)
     */
    void publishOrcamentoRejeitado(OrcamentoRejeitadoEvent event);

    /**
     * Publica evento de compensação - Pagamento falhou (Rollback)
     */
    void publishPagamentoFalhou(PagamentoFalhouEvent event);

    // Métodos de conveniência usando entidades
    void publicarOrcamentoCriado(Orcamento orcamento);

    void publicarOrcamentoAprovado(Orcamento orcamento);

    void publicarOrcamentoRejeitado(Orcamento orcamento);

    void publicarPagamentoRegistrado(Pagamento pagamento);

    void publicarPagamentoConfirmado(Pagamento pagamento);

    void publicarPagamentoEstornado(Pagamento pagamento);
}
