package br.com.grupo99.billingservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para request de criar pagamento
 * 
 * ✅ CLEAN ARCHITECTURE: DTO isolado na application layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePagamentoRequest {

    private UUID orcamentoId;
    private UUID osId;
    private BigDecimal valor;
    private String formaPagamento;
    private String comprovante;
    private String payerEmail;
}
