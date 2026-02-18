package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusOrcamento - Testes Unitários")
class StatusOrcamentoTest {

    @Test
    @DisplayName("PENDENTE pode transicionar para APROVADO, REJEITADO e CANCELADO")
    void pendentePodeTransicionarParaAprovadoRejeitadoCancelado() {
        assertThat(StatusOrcamento.PENDENTE.podeTransicionarPara(StatusOrcamento.APROVADO)).isTrue();
        assertThat(StatusOrcamento.PENDENTE.podeTransicionarPara(StatusOrcamento.REJEITADO)).isTrue();
        assertThat(StatusOrcamento.PENDENTE.podeTransicionarPara(StatusOrcamento.CANCELADO)).isTrue();
    }

    @Test
    @DisplayName("APROVADO não pode transicionar")
    void aprovadoNaoPodeTransicionar() {
        for (StatusOrcamento s : StatusOrcamento.values()) {
            assertThat(StatusOrcamento.APROVADO.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    @DisplayName("REJEITADO não pode transicionar")
    void rejeitadoNaoPodeTransicionar() {
        for (StatusOrcamento s : StatusOrcamento.values()) {
            assertThat(StatusOrcamento.REJEITADO.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    @DisplayName("CANCELADO não pode transicionar")
    void canceladoNaoPodeTransicionar() {
        for (StatusOrcamento s : StatusOrcamento.values()) {
            assertThat(StatusOrcamento.CANCELADO.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    @DisplayName("Deve retornar próximos status permitidos para PENDENTE")
    void deveRetornarProximosStatusParaPendente() {
        List<StatusOrcamento> proximos = StatusOrcamento.PENDENTE.proximosStatusPermitidos();
        assertThat(proximos).containsExactly(StatusOrcamento.APROVADO, StatusOrcamento.REJEITADO, StatusOrcamento.CANCELADO);
    }

    @Test
    @DisplayName("Deve retornar lista vazia para status finais")
    void deveRetornarListaVaziaParaStatusFinais() {
        assertThat(StatusOrcamento.APROVADO.proximosStatusPermitidos()).isEmpty();
        assertThat(StatusOrcamento.REJEITADO.proximosStatusPermitidos()).isEmpty();
        assertThat(StatusOrcamento.CANCELADO.proximosStatusPermitidos()).isEmpty();
    }

    @Test
    @DisplayName("APROVADO, REJEITADO e CANCELADO são estados finais")
    void statusFinaisDevemRetornarTrue() {
        assertThat(StatusOrcamento.APROVADO.isFinal()).isTrue();
        assertThat(StatusOrcamento.REJEITADO.isFinal()).isTrue();
        assertThat(StatusOrcamento.CANCELADO.isFinal()).isTrue();
    }

    @Test
    @DisplayName("PENDENTE não é estado final")
    void pendenteNaoEEstadoFinal() {
        assertThat(StatusOrcamento.PENDENTE.isFinal()).isFalse();
    }

    @Test
    @DisplayName("Deve ter descrição para cada status")
    void deveTerDescricaoParaCadaStatus() {
        assertThat(StatusOrcamento.PENDENTE.getDescricao()).isEqualTo("Pendente aprovação do cliente");
        assertThat(StatusOrcamento.APROVADO.getDescricao()).isEqualTo("Aprovado pelo cliente");
        assertThat(StatusOrcamento.REJEITADO.getDescricao()).isEqualTo("Rejeitado pelo cliente");
        assertThat(StatusOrcamento.CANCELADO.getDescricao()).isEqualTo("Cancelado");
    }
}
