package br.com.grupo99.billingservice.domain.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representando os possíveis status de um Orçamento.
 * Garante transições válidas de estado.
 */
@Getter
public enum StatusOrcamento {

    PENDENTE("Pendente aprovação do cliente"),
    APROVADO("Aprovado pelo cliente"),
    REJEITADO("Rejeitado pelo cliente"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusOrcamento(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Valida se uma transição de status é permitida.
     *
     * @param novoStatus status destino
     * @return true se transição é válida
     */
    public boolean podeTransicionarPara(StatusOrcamento novoStatus) {
        return switch (this) {
            case PENDENTE -> novoStatus == APROVADO || novoStatus == REJEITADO || novoStatus == CANCELADO;
            case APROVADO, REJEITADO, CANCELADO -> false; // Estados finais
        };
    }

    /**
     * Retorna os próximos status possíveis a partir do status atual.
     *
     * @return lista de status válidos
     */
    public List<StatusOrcamento> proximosStatusPermitidos() {
        return switch (this) {
            case PENDENTE -> Arrays.asList(APROVADO, REJEITADO, CANCELADO);
            case APROVADO, REJEITADO, CANCELADO -> List.of();
        };
    }

    /**
     * Verifica se o status é final (não permite mais transições).
     *
     * @return true se é um status final
     */
    public boolean isFinal() {
        return this == APROVADO || this == REJEITADO || this == CANCELADO;
    }
}
