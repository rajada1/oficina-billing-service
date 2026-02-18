package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusPagamento - Testes Unitários")
class StatusPagamentoTest {

    @Test
    @DisplayName("PENDENTE pode transicionar para PROCESSANDO, CONFIRMADO e CANCELADO")
    void pendentePodeTransicionar() {
        assertThat(StatusPagamento.PENDENTE.podeTransicionarPara(StatusPagamento.PROCESSANDO)).isTrue();
        assertThat(StatusPagamento.PENDENTE.podeTransicionarPara(StatusPagamento.CONFIRMADO)).isTrue();
        assertThat(StatusPagamento.PENDENTE.podeTransicionarPara(StatusPagamento.CANCELADO)).isTrue();
        assertThat(StatusPagamento.PENDENTE.podeTransicionarPara(StatusPagamento.ESTORNADO)).isFalse();
    }

    @Test
    @DisplayName("PROCESSANDO pode transicionar para CONFIRMADO, CANCELADO e ESTORNADO")
    void processandoPodeTransicionar() {
        assertThat(StatusPagamento.PROCESSANDO.podeTransicionarPara(StatusPagamento.CONFIRMADO)).isTrue();
        assertThat(StatusPagamento.PROCESSANDO.podeTransicionarPara(StatusPagamento.CANCELADO)).isTrue();
        assertThat(StatusPagamento.PROCESSANDO.podeTransicionarPara(StatusPagamento.ESTORNADO)).isTrue();
        assertThat(StatusPagamento.PROCESSANDO.podeTransicionarPara(StatusPagamento.PENDENTE)).isFalse();
    }

    @Test
    @DisplayName("CONFIRMADO pode transicionar apenas para ESTORNADO")
    void confirmadoPodeTransicionar() {
        assertThat(StatusPagamento.CONFIRMADO.podeTransicionarPara(StatusPagamento.ESTORNADO)).isTrue();
        assertThat(StatusPagamento.CONFIRMADO.podeTransicionarPara(StatusPagamento.CANCELADO)).isFalse();
        assertThat(StatusPagamento.CONFIRMADO.podeTransicionarPara(StatusPagamento.PENDENTE)).isFalse();
    }

    @Test
    @DisplayName("ESTORNADO não pode transicionar")
    void estornadoNaoPodeTransicionar() {
        for (StatusPagamento s : StatusPagamento.values()) {
            assertThat(StatusPagamento.ESTORNADO.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    @DisplayName("CANCELADO não pode transicionar")
    void canceladoNaoPodeTransicionar() {
        for (StatusPagamento s : StatusPagamento.values()) {
            assertThat(StatusPagamento.CANCELADO.podeTransicionarPara(s)).isFalse();
        }
    }

    @Test
    @DisplayName("CONFIRMADO, ESTORNADO e CANCELADO são estados finais")
    void estadosFinais() {
        assertThat(StatusPagamento.CONFIRMADO.isFinal()).isTrue();
        assertThat(StatusPagamento.ESTORNADO.isFinal()).isTrue();
        assertThat(StatusPagamento.CANCELADO.isFinal()).isTrue();
    }

    @Test
    @DisplayName("PENDENTE e PROCESSANDO não são estados finais")
    void estadosNaoFinais() {
        assertThat(StatusPagamento.PENDENTE.isFinal()).isFalse();
        assertThat(StatusPagamento.PROCESSANDO.isFinal()).isFalse();
    }

    @Test
    @DisplayName("Deve ter descrição para cada status")
    void deveTerDescricao() {
        assertThat(StatusPagamento.PENDENTE.getDescricao()).isEqualTo("Pendente confirmação");
        assertThat(StatusPagamento.PROCESSANDO.getDescricao()).isEqualTo("Processando no Mercado Pago");
        assertThat(StatusPagamento.CONFIRMADO.getDescricao()).isEqualTo("Confirmado");
        assertThat(StatusPagamento.ESTORNADO.getDescricao()).isEqualTo("Estornado");
        assertThat(StatusPagamento.CANCELADO.getDescricao()).isEqualTo("Cancelado");
    }
}
