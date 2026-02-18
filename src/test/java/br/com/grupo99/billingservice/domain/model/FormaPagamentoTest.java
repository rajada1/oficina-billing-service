package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FormaPagamento - Testes Unitários")
class FormaPagamentoTest {

    @Test
    @DisplayName("Deve ter todas as formas de pagamento")
    void deveTerTodasAsFormasDePagamento() {
        assertThat(FormaPagamento.values()).hasSize(6);
    }

    @Test
    @DisplayName("Deve ter descrição para cada forma de pagamento")
    void deveTerDescricaoParaCadaForma() {
        assertThat(FormaPagamento.DINHEIRO.getDescricao()).isEqualTo("Dinheiro");
        assertThat(FormaPagamento.CARTAO_CREDITO.getDescricao()).isEqualTo("Cartão de Crédito");
        assertThat(FormaPagamento.CARTAO_DEBITO.getDescricao()).isEqualTo("Cartão de Débito");
        assertThat(FormaPagamento.PIX.getDescricao()).isEqualTo("PIX");
        assertThat(FormaPagamento.TRANSFERENCIA.getDescricao()).isEqualTo("Transferência Bancária");
        assertThat(FormaPagamento.BOLETO.getDescricao()).isEqualTo("Boleto Bancário");
    }

    @Test
    @DisplayName("Deve converter de string via valueOf")
    void deveConverterDeStringViaValueOf() {
        assertThat(FormaPagamento.valueOf("PIX")).isEqualTo(FormaPagamento.PIX);
        assertThat(FormaPagamento.valueOf("DINHEIRO")).isEqualTo(FormaPagamento.DINHEIRO);
        assertThat(FormaPagamento.valueOf("CARTAO_CREDITO")).isEqualTo(FormaPagamento.CARTAO_CREDITO);
    }
}
