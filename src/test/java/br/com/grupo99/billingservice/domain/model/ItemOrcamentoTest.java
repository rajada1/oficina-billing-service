package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemOrcamento - Testes Unitários")
class ItemOrcamentoTest {

    @Test
    @DisplayName("Deve criar ItemOrcamento com 4 parâmetros e calcular valor total")
    void deveCriarItemOrcamentoComQuatroParametros() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.PECA, "Filtro de óleo", 2, BigDecimal.valueOf(50.00));

        assertThat(item.getTipo()).isEqualTo(TipoItem.PECA);
        assertThat(item.getDescricao()).isEqualTo("Filtro de óleo");
        assertThat(item.getQuantidade()).isEqualTo(2);
        assertThat(item.getValorUnitario()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(item.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Deve calcular valor total")
    void deveCalcularValorTotal() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(TipoItem.SERVICO)
                .descricao("Troca de óleo")
                .quantidade(1)
                .valorUnitario(BigDecimal.valueOf(80.00))
                .build();

        BigDecimal valorTotal = item.calcularValorTotal();

        assertThat(valorTotal).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }

    @Test
    @DisplayName("Deve retornar zero quando quantidade é nula")
    void deveRetornarZeroQuandoQuantidadeNula() {
        ItemOrcamento item = ItemOrcamento.builder()
                .quantidade(null)
                .valorUnitario(BigDecimal.TEN)
                .build();

        assertThat(item.calcularValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar zero quando valorUnitario é nulo")
    void deveRetornarZeroQuandoValorUnitarioNulo() {
        ItemOrcamento item = ItemOrcamento.builder()
                .quantidade(5)
                .valorUnitario(null)
                .build();

        assertThat(item.calcularValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Item válido deve retornar true")
    void itemValidoDeveRetornarTrue() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.PECA, "Filtro", 1, BigDecimal.TEN);

        assertThat(item.isValido()).isTrue();
    }

    @Test
    @DisplayName("Item sem tipo deve ser inválido")
    void itemSemTipoDeveSerInvalido() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(null)
                .descricao("Desc")
                .quantidade(1)
                .valorUnitario(BigDecimal.TEN)
                .build();

        assertThat(item.isValido()).isFalse();
    }

    @Test
    @DisplayName("Item sem descrição deve ser inválido")
    void itemSemDescricaoDeveSerInvalido() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(TipoItem.PECA)
                .descricao(null)
                .quantidade(1)
                .valorUnitario(BigDecimal.TEN)
                .build();

        assertThat(item.isValido()).isFalse();
    }

    @Test
    @DisplayName("Item com descrição em branco deve ser inválido")
    void itemComDescricaoEmBrancoDeveSerInvalido() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(TipoItem.PECA)
                .descricao("  ")
                .quantidade(1)
                .valorUnitario(BigDecimal.TEN)
                .build();

        assertThat(item.isValido()).isFalse();
    }

    @Test
    @DisplayName("Item com quantidade zero deve ser inválido")
    void itemComQuantidadeZeroDeveSerInvalido() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(TipoItem.PECA)
                .descricao("Desc")
                .quantidade(0)
                .valorUnitario(BigDecimal.TEN)
                .build();

        assertThat(item.isValido()).isFalse();
    }

    @Test
    @DisplayName("Item com valor zero deve ser inválido")
    void itemComValorZeroDeveSerInvalido() {
        ItemOrcamento item = ItemOrcamento.builder()
                .tipo(TipoItem.PECA)
                .descricao("Desc")
                .quantidade(1)
                .valorUnitario(BigDecimal.ZERO)
                .build();

        assertThat(item.isValido()).isFalse();
    }

    @Test
    @DisplayName("Deve criar com construtor padrão")
    void deveCriarComConstrutorPadrao() {
        ItemOrcamento item = new ItemOrcamento();

        assertThat(item.getTipo()).isNull();
        assertThat(item.getDescricao()).isNull();
        assertThat(item.getQuantidade()).isNull();
        assertThat(item.getValorUnitario()).isNull();
        assertThat(item.getValorTotal()).isNull();
    }
}
