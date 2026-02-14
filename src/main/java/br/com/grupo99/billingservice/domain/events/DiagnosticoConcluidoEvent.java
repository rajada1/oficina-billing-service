package br.com.grupo99.billingservice.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticoConcluidoEvent {
    private UUID osId;
    private UUID execucaoId;
    private String diagnostico;
    private BigDecimal valorEstimado;
    private LocalDateTime timestamp;
    private String eventType = "DIAGNOSTICO_CONCLUIDO";
}
