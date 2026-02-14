package br.com.grupo99.billingservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO para response de orçamento
 * 
 * ✅ CLEAN ARCHITECTURE: DTO isolado na application layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoResponse {

    private UUID id;
    private UUID osId;
    private String status;
    private BigDecimal valorTotal;
    private List<ItemOrcamentoResponse> itens;
    private String observacao;
    private Instant dataGeracao;
    private Instant dataAprovacao;
    private Instant dataRejeicao;
    private Integer historicoCount;

    /**
     * Item do orçamento (nested DTO)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOrcamentoResponse {
        private String descricao;
        private BigDecimal valor;
        private Integer quantidade;
    }
}
