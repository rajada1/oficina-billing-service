package br.com.grupo99.billingservice.domain.model;

import lombok.Getter;

/**
 * Enum representando os possíveis status de um Pagamento.
 */
@Getter
public enum StatusPagamento {

    PENDENTE("Pendente confirmação"),
    CONFIRMADO("Confirmado"),
    ESTORNADO("Estornado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Valida se uma transição de status é permitida.
     *
     * @param novoStatus status destino
     * @return true se transição é válida
     */
    public boolean podeTransicionarPara(StatusPagamento novoStatus) {
        return switch (this) {
            case PENDENTE -> novoStatus == CONFIRMADO || novoStatus == CANCELADO;
            case CONFIRMADO -> novoStatus == ESTORNADO;
            case ESTORNADO, CANCELADO -> false; // Estados finais
        };
    }

    /**
     * Verifica se o status é final.
     *
     * @return true se é um status final
     */
    public boolean isFinal() {
        return this == CONFIRMADO || this == ESTORNADO || this == CANCELADO;
    }
}
