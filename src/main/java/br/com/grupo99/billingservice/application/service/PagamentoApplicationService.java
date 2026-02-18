package br.com.grupo99.billingservice.application.service;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.application.mapper.PagamentoMapper;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPaymentResult;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPreferenceResult;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import br.com.grupo99.billingservice.domain.repository.PagamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application Service para Pagamento
 * 
 * ✅ CLEAN ARCHITECTURE: Orquestração de use cases na camada application
 * ✅ MERCADO PAGO: Integração via Preference (link de pagamento) + checagem
 */
@Slf4j
@Service
@Transactional
public class PagamentoApplicationService {

    private final PagamentoRepository pagamentoRepository;
    private final BillingEventPublisherPort eventPublisher;
    private final MercadoPagoPort mercadoPagoPort;
    private final PagamentoMapper mapper;

    public PagamentoApplicationService(
            PagamentoRepository pagamentoRepository,
            BillingEventPublisherPort eventPublisher,
            MercadoPagoPort mercadoPagoPort,
            PagamentoMapper mapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.eventPublisher = eventPublisher;
        this.mercadoPagoPort = mercadoPagoPort;
        this.mapper = mapper;
    }

    /**
     * Use Case: Registrar Pagamento — Gera link de pagamento via Mercado Pago
     *
     * Fluxo:
     * 1. Cria o pagamento no domain (status PENDENTE)
     * 2. Cria preferência no Mercado Pago (gera link de pagamento)
     * 3. Salva preferenceId e initPoint no pagamento
     * 4. Persiste no DynamoDB
     * 5. Publica evento via Kafka
     * 6. Retorna response com link de pagamento para enviar ao cliente
     */
    public PagamentoResponse registrar(CreatePagamentoRequest request) {
        log.info("Registrando pagamento para orçamento: {}", request.getOrcamentoId());

        // 1. Domain: criar pagamento
        Pagamento pagamento = mapper.toDomain(request);

        // 2. Criar preferência no Mercado Pago (gera link de pagamento)
        String descricao = String.format("Pagamento OS - Orçamento %s", request.getOrcamentoId());
        String payerEmail = request.getPayerEmail() != null ? request.getPayerEmail() : "test@test.com";
        String externalReference = pagamento.getId().toString();

        MercadoPagoPreferenceResult prefResult = mercadoPagoPort.criarPreferencia(
                descricao,
                request.getValor(),
                payerEmail,
                externalReference);

        // 3. Salvar dados do MP no pagamento
        pagamento.setMercadoPagoPreferenceId(prefResult.preferenceId());
        pagamento.setInitPoint(prefResult.initPoint());

        // Status continua PENDENTE — aguardando o cliente pagar via link

        // 4. Persistência
        Pagamento saved = pagamentoRepository.save(pagamento);
        log.info("Pagamento registrado com ID: {}, Preference ID: {}, Link: {}",
                saved.getId(), prefResult.preferenceId(), prefResult.initPoint());

        // 5. Events
        eventPublisher.publicarPagamentoRegistrado(saved);

        // 6. Response com link de pagamento
        PagamentoResponse response = mapper.toResponse(saved);
        return response;
    }

    /**
     * Use Case: Checar status de pagamento no Mercado Pago
     *
     * Consulta no MP se o pagamento já foi realizado pelo cliente.
     * Usa external_reference (pagamentoId) para buscar o pagamento no MP.
     */
    public PagamentoResponse checarPagamento(UUID id) {
        log.info("Checando pagamento no Mercado Pago: {}", id);

        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado: " + id));

        // Se já está confirmado ou em estado final, retorna direto
        if (pagamento.getStatus().isFinal()) {
            log.info("Pagamento {} já está em estado final: {}", id, pagamento.getStatus());
            return mapper.toResponse(pagamento);
        }

        // Buscar pagamento no MP por external_reference (nosso ID de pagamento)
        MercadoPagoPaymentResult mpResult = mercadoPagoPort.buscarPagamentoPorReferencia(id.toString());

        log.info("Resultado checagem MP para {}: status={}, paymentId={}",
                id, mpResult.status(), mpResult.paymentId());

        // Atualizar status baseado no retorno do MP
        String mpStatus = mpResult.status();

        switch (mpStatus != null ? mpStatus.toLowerCase() : "not_found") {
            case "approved" -> {
                if (pagamento.getStatus() != StatusPagamento.CONFIRMADO) {
                    if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
                        pagamento.processar(mpResult.paymentId());
                    }
                    pagamento.confirmar();
                    pagamentoRepository.save(pagamento);
                    eventPublisher.publicarPagamentoConfirmado(pagamento);
                    log.info("✅ Pagamento {} confirmado! MP Payment ID: {}", id, mpResult.paymentId());
                }
            }
            case "pending", "in_process" -> {
                if (pagamento.getStatus() == StatusPagamento.PENDENTE && mpResult.paymentId() != null) {
                    pagamento.processar(mpResult.paymentId());
                    pagamentoRepository.save(pagamento);
                    log.info("⏳ Pagamento {} está processando no MP. Payment ID: {}",
                            id, mpResult.paymentId());
                }
            }
            case "rejected", "cancelled" -> {
                if (pagamento.getStatus() != StatusPagamento.CANCELADO) {
                    pagamento.cancelar();
                    pagamentoRepository.save(pagamento);
                    log.info("❌ Pagamento {} cancelado/rejeitado pelo MP (status: {})",
                            id, mpStatus);
                }
            }
            case "refunded" -> {
                if (pagamento.getStatus() != StatusPagamento.ESTORNADO) {
                    pagamento.estornar("Estorno via Mercado Pago");
                    pagamentoRepository.save(pagamento);
                    eventPublisher.publicarPagamentoEstornado(pagamento);
                    log.info("↩️ Pagamento {} estornado via MP", id);
                }
            }
            case "not_found" -> {
                log.info("🔍 Nenhum pagamento encontrado no MP para referência: {}. Cliente ainda não pagou.", id);
            }
            default -> log.info("Status MP '{}' não requer ação no pagamento {}", mpStatus, id);
        }

        return mapper.toResponse(pagamento);
    }

    /**
     * Use Case: Processar Webhook do Mercado Pago
     *
     * Chamado quando o MP envia notificação de atualização de pagamento.
     */
    public void processarWebhook(Long mercadoPagoPaymentId) {
        log.info("Processando webhook do Mercado Pago para payment_id: {}", mercadoPagoPaymentId);

        // 1. Consultar status no MP
        MercadoPagoPaymentResult mpResult = mercadoPagoPort.consultarPagamento(mercadoPagoPaymentId);

        // 2. Buscar pagamento local pelo MP ID
        Pagamento pagamento = pagamentoRepository.findByMercadoPagoPaymentId(mercadoPagoPaymentId)
                .orElseThrow(() -> new RuntimeException(
                        "Pagamento não encontrado para MP ID: " + mercadoPagoPaymentId));

        // 3. Atualizar status baseado no retorno do MP
        String mpStatus = mpResult.status();
        log.info("Status MP: {} para pagamento local: {}", mpStatus, pagamento.getId());

        switch (mpStatus.toLowerCase()) {
            case "approved" -> {
                if (pagamento.getStatus() != StatusPagamento.CONFIRMADO) {
                    pagamento.confirmar();
                    pagamentoRepository.save(pagamento);
                    eventPublisher.publicarPagamentoConfirmado(pagamento);
                    log.info("Pagamento {} confirmado via webhook", pagamento.getId());
                }
            }
            case "rejected", "cancelled" -> {
                if (pagamento.getStatus() != StatusPagamento.CANCELADO) {
                    pagamento.cancelar();
                    pagamentoRepository.save(pagamento);
                    log.info("Pagamento {} cancelado via webhook (MP status: {})",
                            pagamento.getId(), mpStatus);
                }
            }
            case "refunded" -> {
                if (pagamento.getStatus() != StatusPagamento.ESTORNADO) {
                    pagamento.estornar("Estorno via Mercado Pago");
                    pagamentoRepository.save(pagamento);
                    eventPublisher.publicarPagamentoEstornado(pagamento);
                    log.info("Pagamento {} estornado via webhook", pagamento.getId());
                }
            }
            default -> log.info("Status MP '{}' não requer ação no pagamento {}", mpStatus, pagamento.getId());
        }
    }

    /**
     * Use Case: Listar todos os pagamentos
     */
    public java.util.List<PagamentoResponse> listarTodos() {
        log.info("Listando todos os pagamentos");
        return pagamentoRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Use Case: Confirmar Pagamento manualmente
     */
    public PagamentoResponse confirmar(UUID id) {
        log.info("Confirmando pagamento: {}", id);

        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado: " + id));

        // Lógica de domínio
        pagamento.confirmar();

        // Persistir
        Pagamento updated = pagamentoRepository.save(pagamento);

        // Publicar evento
        eventPublisher.publicarPagamentoConfirmado(updated);

        log.info("Pagamento {} confirmado", id);
        return mapper.toResponse(updated);
    }

    /**
     * Use Case: Estornar Pagamento
     */
    public PagamentoResponse estornar(UUID id, String motivo) {
        log.info("Estornando pagamento: {}, motivo: {}", id, motivo);

        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado: " + id));

        pagamento.estornar(motivo);
        Pagamento updated = pagamentoRepository.save(pagamento);

        eventPublisher.publicarPagamentoEstornado(updated);

        return mapper.toResponse(updated);
    }

    /**
     * Use Case: Cancelar Pagamento
     */
    public void cancelar(UUID id) {
        log.info("Cancelando pagamento: {}", id);

        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado: " + id));

        pagamento.cancelar();
        pagamentoRepository.save(pagamento);

        log.info("Pagamento {} cancelado", id);
    }
}
