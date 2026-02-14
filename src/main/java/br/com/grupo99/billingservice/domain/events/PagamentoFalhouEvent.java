package br.com.grupo99.billingservice.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de compensação - Pagamento falhou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoFalhouEvent {
    private UUID pagamentoId;
    private UUID orcamentoId;
    private UUID osId;
    private String motivo;
    private String mensagemErro;
    private String codigoErro;
    private BigDecimal valorTentado;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "PAGAMENTO_FALHOU";
}
