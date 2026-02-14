package br.com.grupo99.billingservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value Object representando uma entrada do histórico de mudanças de status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoStatus {

    private StatusOrcamento statusAnterior;

    private StatusOrcamento novoStatus;

    private Instant data;

    private String usuario;

    private String observacao;

    /**
     * Cria uma entrada de histórico com data atual.
     *
     * @param statusAnterior status anterior
     * @param novoStatus     novo status
     * @param usuario        usuário que realizou a mudança
     * @param observacao     observação opcional
     * @return instância de HistoricoStatus
     */
    public static HistoricoStatus criar(
            StatusOrcamento statusAnterior,
            StatusOrcamento novoStatus,
            String usuario,
            String observacao) {
        return HistoricoStatus.builder()
                .statusAnterior(statusAnterior)
                .novoStatus(novoStatus)
                .data(Instant.now())
                .usuario(usuario)
                .observacao(observacao)
                .build();
    }

    /**
     * Retorna o novo status (para compatibilidade com testes).
     *
     * @return novo status
     */
    public StatusOrcamento getStatusNovo() {
        return this.novoStatus;
    }

    /**
     * Retorna a data da transição (para compatibilidade com testes).
     *
     * @return data da transição
     */
    public Instant getDataTransicao() {
        return this.data;
    }
}
