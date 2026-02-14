package br.com.grupo99.billingservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para request de criar orçamento
 * 
 * ✅ CLEAN ARCHITECTURE: DTO isolado na application layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrcamentoRequest {

    private UUID osId;
    private List<ItemOrcamentoRequest> itens;
    private String observacao;

    /**
     * Item do orçamento (nested DTO)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOrcamentoRequest {
        private String descricao;
        private java.math.BigDecimal valor;
        private Integer quantidade;
    }
}
