package br.com.grupo99.billingservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Value Object representando um item de orçamento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOrcamento {

    private TipoItem tipo;

    private String descricao;

    private Integer quantidade;

    private BigDecimal valorUnitario;

    private BigDecimal valorTotal;

    /**
     * Construtor para testes (calcula automaticamente o valorTotal).
     *
     * @param tipo          tipo do item
     * @param descricao     descrição
     * @param quantidade    quantidade
     * @param valorUnitario valor unitário
     */
    public ItemOrcamento(TipoItem tipo, String descricao, Integer quantidade, BigDecimal valorUnitario) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.valorTotal = calcularValorTotal();
    }

    /**
     * Calcula o valor total do item.
     *
     * @return valor total (quantidade * valorUnitario)
     */
    public BigDecimal calcularValorTotal() {
        if (quantidade == null || valorUnitario == null) {
            return BigDecimal.ZERO;
        }
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /**
     * Valida se o item está consistente.
     *
     * @return true se item é válido
     */
    public boolean isValido() {
        return tipo != null
                && descricao != null && !descricao.isBlank()
                && quantidade != null && quantidade > 0
                && valorUnitario != null && valorUnitario.compareTo(BigDecimal.ZERO) > 0;
    }
}
