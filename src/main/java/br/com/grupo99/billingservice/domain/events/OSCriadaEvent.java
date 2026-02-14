package br.com.grupo99.billingservice.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OSCriadaEvent {
    private UUID osId;
    private UUID clienteId;
    private UUID veiculoId;
    private String descricao;
    private LocalDateTime timestamp;
    private String eventType = "OS_CRIADA";
}
