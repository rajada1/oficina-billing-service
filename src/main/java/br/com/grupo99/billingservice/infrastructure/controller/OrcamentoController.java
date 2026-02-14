package br.com.grupo99.billingservice.infrastructure.controller;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.application.service.OrcamentoApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller para Orcamento
 * 
 * ✅ CLEAN ARCHITECTURE: Controller chama Application Service
 * - Não contém lógica de negócio
 * - Apenas adapta HTTP para DTOs
 * - Delega para application service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orcamentos")
public class OrcamentoController {

    private final OrcamentoApplicationService service;

    public OrcamentoController(OrcamentoApplicationService service) {
        this.service = service;
    }

    /**
     * POST /api/v1/orcamentos
     * Criar novo orçamento
     */
    @PostMapping
    public ResponseEntity<OrcamentoResponse> criar(
            @RequestBody CreateOrcamentoRequest request) {
        log.info("POST /orcamentos - criando novo orçamento para OS: {}", request.getOsId());

        try {
            OrcamentoResponse response = service.criar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Erro ao criar orçamento", e);
            throw e;
        }
    }

    /**
     * GET /api/v1/orcamentos/{id}
     * Obter orçamento por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponse> obterPorId(
            @PathVariable UUID id) {
        log.info("GET /orcamentos/{} - obtendo orçamento", id);

        try {
            OrcamentoResponse response = service.obterPorId(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao obter orçamento", e);
            throw e;
        }
    }

    /**
     * PUT /api/v1/orcamentos/{id}/aprovar
     * Aprovar orçamento
     */
    @PutMapping("/{id}/aprovar")
    public ResponseEntity<OrcamentoResponse> aprovar(
            @PathVariable UUID id) {
        log.info("PUT /orcamentos/{}/aprovar - aprovando orçamento", id);

        try {
            OrcamentoResponse response = service.aprovar(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao aprovar orçamento", e);
            throw e;
        }
    }

    /**
     * PUT /api/v1/orcamentos/{id}/rejeitar
     * Rejeitar orçamento
     */
    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<OrcamentoResponse> rejeitar(
            @PathVariable UUID id,
            @RequestParam(required = false) String motivo) {
        log.info("PUT /orcamentos/{}/rejeitar - rejeitando orçamento", id);

        try {
            OrcamentoResponse response = service.rejeitar(id, motivo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao rejeitar orçamento", e);
            throw e;
        }
    }

    /**
     * DELETE /api/v1/orcamentos/{id}
     * Cancelar orçamento
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID id) {
        log.info("DELETE /orcamentos/{} - cancelando orçamento", id);

        try {
            service.cancelar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao cancelar orçamento", e);
            throw e;
        }
    }
}
