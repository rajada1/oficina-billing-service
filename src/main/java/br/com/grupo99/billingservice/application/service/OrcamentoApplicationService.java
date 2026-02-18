package br.com.grupo99.billingservice.application.service;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.application.mapper.OrcamentoMapper;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.repository.OrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.List;
import java.util.UUID;

/**
 * Application Service para Orçamento
 * 
 * ✅ CLEAN ARCHITECTURE: Orquestração de use cases na camada application
 * - Não contém lógica de negócio (fica no domain)
 * - Coordena chamadas entre domain e infrastructure
 * - Gerencia transações
 */
@Slf4j
@Service
@Transactional
public class OrcamentoApplicationService {

    private final OrcamentoRepository orcamentoRepository;
    private final BillingEventPublisherPort eventPublisher;
    private final OrcamentoMapper mapper;

    public OrcamentoApplicationService(
            OrcamentoRepository orcamentoRepository,
            BillingEventPublisherPort eventPublisher,
            OrcamentoMapper mapper) {
        this.orcamentoRepository = orcamentoRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    /**
     * Use Case: Criar Orçamento
     * 
     * Fluxo:
     * 1. Converter DTO em domain model
     * 2. Persistir usando repository
     * 3. Publicar evento via publisher
     * 4. Retornar response
     */
    public OrcamentoResponse criar(CreateOrcamentoRequest request) {
        log.info("Criando orçamento para OS: {}", request.getOsId());

        // 1. Domain: criar agregado
        Orcamento orcamento = mapper.toDomain(request);

        // 2. Persistência: usar repository (não direto no BD)
        Orcamento saved = orcamentoRepository.save(orcamento);
        log.info("Orçamento criado com ID: {}", saved.getId());

        // 3. Events: publicar eventos de domínio
        eventPublisher.publicarOrcamentoCriado(saved);

        // 4. Response: converter domain para DTO
        return mapper.toResponse(saved);
    }

    /**
     * Use Case: Buscar Orçamento por ID
     */
    public OrcamentoResponse obterPorId(UUID id) {
        log.info("Buscando orçamento: {}", id);

        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado: " + id));

        return mapper.toResponse(orcamento);
    }

    /**
     * Use Case: Aprovar Orçamento
     * 
     * Fluxo:
     * 1. Buscar orçamento
     * 2. Executar lógica de domínio (aprovar())
     * 3. Persistir mudanças
     * 4. Publicar evento
     * 5. Retornar response
     */
    public OrcamentoResponse aprovar(UUID id) {
        log.info("Aprovando orçamento: {}", id);

        // 1. Buscar domain
        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado: " + id));

        // 2. Executar lógica de domínio
        orcamento.aprovar();
        log.info("Orçamento {} aprovado", id);

        // 3. Persistir
        Orcamento updated = orcamentoRepository.save(orcamento);

        // 4. Publicar evento
        eventPublisher.publicarOrcamentoAprovado(updated);

        // 5. Response
        return mapper.toResponse(updated);
    }

    /**
     * Use Case: Rejeitar Orçamento
     */
    public OrcamentoResponse rejeitar(UUID id, String motivo) {
        log.info("Rejeitando orçamento: {}, motivo: {}", id, motivo);

        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado: " + id));

        orcamento.rejeitar();
        Orcamento updated = orcamentoRepository.save(orcamento);

        eventPublisher.publicarOrcamentoRejeitado(updated);

        return mapper.toResponse(updated);
    }

    /**
     * Use Case: Cancelar Orçamento
     */
    public void cancelar(UUID id) {
        log.info("Cancelando orçamento: {}", id);

        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado: " + id));

        orcamento.cancelar();
        orcamentoRepository.save(orcamento);

        log.info("Orçamento {} cancelado", id);
    }

    /**
     * Use Case: Cancelar Orçamento por OS ID (para eventos Kafka)
     */
    public void cancelarPorOs(UUID osId, String motivo) {
        log.info("Cancelando orçamento por OS: {}, motivo: {}", osId, motivo);

        Orcamento orcamento = orcamentoRepository.findByOsId(osId)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado para OS: " + osId));

        orcamento.cancelar();
        orcamentoRepository.save(orcamento);

        log.info("Orçamento {} cancelado para OS: {}", orcamento.getId(), osId);
    }

    /**
     * Use Case: Listar todos os orçamentos
     */
    public List<OrcamentoResponse> listarTodos() {
        log.info("Listando todos os orçamentos");
        return orcamentoRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
