package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.application.service.OrcamentoApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingEventListener - Testes unitários")
class BillingEventListenerTest {

    @Mock
    private OrcamentoApplicationService orcamentoService;

    @InjectMocks
    private BillingEventListener listener;

    @Test
    @DisplayName("Deve cancelar orçamento quando OS é cancelada")
    void deveCancelarOrcamentoQuandoOSCancelada() {
        UUID osId = UUID.randomUUID();

        listener.handleOSCancelada(osId, "OS cancelada pelo cliente");

        verify(orcamentoService).cancelar(osId);
    }

    @Test
    @DisplayName("Deve tratar exceção ao cancelar orçamento no handleOSCancelada")
    void deveTratarExcecaoNoCancelamento() {
        UUID osId = UUID.randomUUID();
        doThrow(new RuntimeException("Orçamento não encontrado")).when(orcamentoService).cancelar(osId);

        // Não deve lançar exceção (tratada internamente)
        listener.handleOSCancelada(osId, "OS cancelada");

        verify(orcamentoService).cancelar(osId);
    }

    @Test
    @DisplayName("Deve cancelar orçamento quando execução falha")
    void deveCancelarOrcamentoQuandoExecucaoFalha() {
        UUID osId = UUID.randomUUID();

        listener.handleExecucaoFalhou(osId, "Execução falhou");

        verify(orcamentoService).cancelar(osId);
    }

    @Test
    @DisplayName("Deve tratar exceção ao cancelar orçamento no handleExecucaoFalhou")
    void deveTratarExcecaoNoHandleExecucaoFalhou() {
        UUID osId = UUID.randomUUID();
        doThrow(new RuntimeException("Erro")).when(orcamentoService).cancelar(osId);

        // Não deve lançar exceção (tratada internamente)
        listener.handleExecucaoFalhou(osId, "Execução falhou");

        verify(orcamentoService).cancelar(osId);
    }
}
