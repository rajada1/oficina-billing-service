package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.application.service.OrcamentoApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event Listener - Compensação Saga Pattern
 * 
 * ✅ CLEAN ARCHITECTURE: Event Listener coordena Application Services
 * - Apenas métodos de compensação para eventos de falha
 * - Delega toda lógica para camada de aplicação
 */
@Slf4j
@Component
public class BillingEventListener {

    private final OrcamentoApplicationService orcamentoService;

    public BillingEventListener(OrcamentoApplicationService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    /**
     * COMPENSAÇÃO: Cancela orçamento quando OS é cancelada
     * Este método é chamado ao receber evento OS_CANCELADA
     * 
     * ✅ CLEAN ARCHITECTURE: Delega para Application Service
     */
    public void handleOSCancelada(UUID osId, String motivo) {
        try {
            log.warn("🔄 Iniciando compensação: Cancelando orçamento para OS: {}", osId);

            // Coordena reversão via Application Service
            orcamentoService.cancelar(osId);
            log.warn("✅ Compensação concluída: Orçamento cancelado para OS: {}", osId);

        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO na compensação do orçamento para OS {}: {}", osId, e.getMessage(), e);
            // Alerta crítico - necessita intervenção manual
        }
    }

    /**
     * COMPENSAÇÃO: Reverte orçamento para AGUARDANDO_APROVACAO se execução falhar
     * 
     * ✅ CLEAN ARCHITECTURE: Delega para Application Service
     */
    public void handleExecucaoFalhou(UUID osId, String motivo) {
        try {
            log.warn("🔄 Iniciando compensação: Cancelando orçamento aprovado para OS: {}", osId);

            // Coordena reversão via Application Service
            orcamentoService.cancelar(osId);

            log.warn("✅ Compensação concluída: Orçamento cancelado devido à falha na execução para OS: {}", osId);

        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO na compensação do orçamento para OS {}: {}", osId, e.getMessage(), e);
        }
    }
}
