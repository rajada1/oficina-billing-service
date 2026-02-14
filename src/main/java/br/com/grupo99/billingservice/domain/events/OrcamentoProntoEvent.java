package br.com.grupo99.billingservice.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoProntoEvent {
    private UUID orcamentoId;
    private UUID osId;
    private BigDecimal valorTotal;
    @Builder.Default
    private Integer prazoValidadeDias = 7;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "ORCAMENTO_PRONTO";
}
