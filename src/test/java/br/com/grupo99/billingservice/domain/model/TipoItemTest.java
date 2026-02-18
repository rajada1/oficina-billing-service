package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TipoItem - Testes unitários")
class TipoItemTest {

    @Test
    @DisplayName("Deve conter todos os tipos esperados")
    void deveConterTodosOsTipos() {
        TipoItem[] valores = TipoItem.values();
        assertThat(valores).hasSize(4);
        assertThat(valores).containsExactlyInAnyOrder(
                TipoItem.SERVICO,
                TipoItem.PECA,
                TipoItem.DIAGNOSTICO,
                TipoItem.MAO_DE_OBRA
        );
    }

    @Test
    @DisplayName("SERVICO deve ter descrição 'Serviço'")
    void servicoDeveRetornarDescricaoCorreta() {
        assertThat(TipoItem.SERVICO.getDescricao()).isEqualTo("Serviço");
    }

    @Test
    @DisplayName("PECA deve ter descrição 'Peça'")
    void pecaDeveRetornarDescricaoCorreta() {
        assertThat(TipoItem.PECA.getDescricao()).isEqualTo("Peça");
    }

    @Test
    @DisplayName("DIAGNOSTICO deve ter descrição 'Diagnóstico'")
    void diagnosticoDeveRetornarDescricaoCorreta() {
        assertThat(TipoItem.DIAGNOSTICO.getDescricao()).isEqualTo("Diagnóstico");
    }

    @Test
    @DisplayName("MAO_DE_OBRA deve ter descrição 'Mão de Obra'")
    void maoDeObraDeveRetornarDescricaoCorreta() {
        assertThat(TipoItem.MAO_DE_OBRA.getDescricao()).isEqualTo("Mão de Obra");
    }

    @Test
    @DisplayName("Deve converter por valueOf corretamente")
    void deveConverterPorValueOf() {
        assertThat(TipoItem.valueOf("SERVICO")).isEqualTo(TipoItem.SERVICO);
        assertThat(TipoItem.valueOf("PECA")).isEqualTo(TipoItem.PECA);
        assertThat(TipoItem.valueOf("DIAGNOSTICO")).isEqualTo(TipoItem.DIAGNOSTICO);
        assertThat(TipoItem.valueOf("MAO_DE_OBRA")).isEqualTo(TipoItem.MAO_DE_OBRA);
    }
}
