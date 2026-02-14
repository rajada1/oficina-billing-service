package br.com.grupo99.billingservice.application.service;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.application.mapper.PagamentoMapper;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.repository.PagamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application Service para Pagamento
 * 
 * ✅ CLEAN ARCHITECTURE: Orquestração de use cases na camada application
 */
@Slf4j
@Service
@Transactional
public class PagamentoApplicationService {

    private final PagamentoRepository pagamentoRepository;
    private final BillingEventPublisher eventPublisher;
    private final PagamentoMapper mapper;

    public PagamentoApplicationService(
            PagamentoRepository pagamentoRepository,
            BillingEventPublisher eventPublisher,
            PagamentoMapper mapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    /**
     * Use Case: Registrar Pagamento
     */
    public PagamentoResponse registrar(CreatePagamentoRequest request) {
        log.info("Registrando pagamento para orçamento: {}", request.getOrcamentoId());

        // 1. Domain: criar pagamento
        Pagamento pagamento = mapper.toDomain(request);

        // 2. Persistência
        Pagamento saved = pagamentoRepository.save(pagamento);
        log.info("Pagamento registrado com ID: {}", saved.getId());

        // 3. Events
        eventPublisher.publicarPagamentoRegistrado(saved);

        // 4. Response
        return mapper.toResponse(saved);
    }

    /**
     * Use Case: Confirmar Pagamento
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
