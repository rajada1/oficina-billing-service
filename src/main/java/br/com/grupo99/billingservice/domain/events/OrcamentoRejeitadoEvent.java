package br.com.grupo99.billingservice.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de compensação - Orçamento foi rejeitado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoRejeitadoEvent {
    private UUID orcamentoId;
    private UUID osId;
    private String motivo;
    private String rejeitadoPor;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "ORCAMENTO_REJEITADO";
}
