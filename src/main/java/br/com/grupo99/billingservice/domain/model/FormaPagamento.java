package br.com.grupo99.billingservice.domain.model;

import lombok.Getter;

/**
 * Enum representando as formas de pagamento aceitas.
 */
@Getter
public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    PIX("PIX"),
    TRANSFERENCIA("Transferência Bancária"),
    BOLETO("Boleto Bancário");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }
}
