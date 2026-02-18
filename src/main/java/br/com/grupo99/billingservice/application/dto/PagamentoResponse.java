package br.com.grupo99.billingservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para response de pagamento
 * 
 * ✅ CLEAN ARCHITECTURE: DTO isolado na application layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponse {

    private UUID id;
    private UUID orcamentoId;
    private UUID osId;
    private String status;
    private BigDecimal valor;
    private String formaPagamento;
    private String comprovante;
    private Instant dataPagamento;
    private Instant dataEstorno;
    private String motivoEstorno;
    private Long mercadoPagoPaymentId;
    private String mercadoPagoPreferenceId;
    private String initPoint;
    private String qrCode;
    private String ticketUrl;
}
