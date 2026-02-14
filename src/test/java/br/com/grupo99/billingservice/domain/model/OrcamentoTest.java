package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Orcamento - Testes Unitários")
class OrcamentoTest {

    @Test
    @DisplayName("Deve criar orçamento com status PENDENTE")
    void deveCriarOrcamentoComStatusPendente() {
        // Arrange
        UUID osId = UUID.randomUUID();

        // Act
        Orcamento orcamento = Orcamento.criar(osId, "Orçamento para reparo");

        // Assert
        assertNotNull(orcamento);
        assertNotNull(orcamento.getId());
        assertEquals(osId, orcamento.getOsId());
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
        assertEquals(BigDecimal.ZERO, orcamento.getValorTotal());
        assertTrue(orcamento.getItens().isEmpty());
    }

    @Test
    @DisplayName("Deve adicionar item ao orçamento")
    void deveAdicionarItemAoOrcamento() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        ItemOrcamento item = new ItemOrcamento(
                TipoItem.SERVICO,
                "Troca de óleo",
                1,
                new BigDecimal("150.00"));

        // Act
        orcamento.adicionarItem(item);

        // Assert
        assertEquals(1, orcamento.getItens().size());
        assertEquals(new BigDecimal("150.00"), orcamento.getValorTotal());
    }

    @Test
    @DisplayName("Deve calcular valor total corretamente com múltiplos itens")
    void deveCalcularValorTotalCorretamenteComMultiplosItens() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");

        ItemOrcamento item1 = new ItemOrcamento(
                TipoItem.SERVICO,
                "Troca de óleo",
                1,
                new BigDecimal("150.00"));

        ItemOrcamento item2 = new ItemOrcamento(
                TipoItem.PECA,
                "Filtro de óleo",
                2,
                new BigDecimal("35.50"));

        ItemOrcamento item3 = new ItemOrcamento(
                TipoItem.MAO_DE_OBRA,
                "Instalação",
                3,
                new BigDecimal("50.00"));

        // Act
        orcamento.adicionarItem(item1);
        orcamento.adicionarItem(item2);
        orcamento.adicionarItem(item3);

        // Assert
        assertEquals(3, orcamento.getItens().size());
        // 150 + (2 * 35.50) + (3 * 50) = 150 + 71 + 150 = 371
        assertEquals(new BigDecimal("371.00"), orcamento.getValorTotal());
    }

    @Test
    @DisplayName("Deve aprovar orçamento PENDENTE")
    void deveAprovarOrcamentoPendente() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");

        // Act
        orcamento.aprovar();

        // Assert
        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
        assertEquals(2, orcamento.getHistorico().size()); // 1 criação + 1 aprovação
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getHistorico().get(1).getStatusAnterior());
        assertEquals(StatusOrcamento.APROVADO, orcamento.getHistorico().get(1).getStatusNovo());
    }

    @Test
    @DisplayName("Deve rejeitar orçamento PENDENTE")
    void deveRejeitarOrcamentoPendente() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");

        // Act
        orcamento.rejeitar();

        // Assert
        assertEquals(StatusOrcamento.REJEITADO, orcamento.getStatus());
        assertEquals(2, orcamento.getHistorico().size()); // 1 criação + 1 rejeição
    }

    @Test
    @DisplayName("Deve cancelar orçamento não finalizado")
    void deveCancelarOrcamentoNaoFinalizado() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");

        // Act
        orcamento.cancelar();

        // Assert
        assertEquals(StatusOrcamento.CANCELADO, orcamento.getStatus());
    }

    @Test
    @DisplayName("Não deve aprovar orçamento já aprovado")
    void naoDeveAprovarOrcamentoJaAprovado() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        orcamento.aprovar();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orcamento.aprovar());
    }

    @Test
    @DisplayName("Não deve aprovar orçamento rejeitado")
    void naoDeveAprovarOrcamentoRejeitado() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        orcamento.rejeitar();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orcamento.aprovar());
    }

    @Test
    @DisplayName("Não deve cancelar orçamento aprovado")
    void naoDeveCancelarOrcamentoAprovado() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        orcamento.aprovar();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orcamento.cancelar());
    }

    @Test
    @DisplayName("Deve recalcular valor total ao adicionar item")
    void deveRecalcularValorTotalAoAdicionarItem() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        ItemOrcamento item1 = new ItemOrcamento(TipoItem.SERVICO, "Serviço 1", 1, new BigDecimal("100.00"));
        ItemOrcamento item2 = new ItemOrcamento(TipoItem.PECA, "Peça 1", 3, new BigDecimal("25.00"));

        // Act
        orcamento.adicionarItem(item1);
        assertEquals(new BigDecimal("100.00"), orcamento.getValorTotal());

        orcamento.adicionarItem(item2);

        // Assert
        assertEquals(new BigDecimal("175.00"), orcamento.getValorTotal());
    }

    @Test
    @DisplayName("Deve manter histórico de mudanças")
    void deveManterHistoricoDeMudancas() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");

        // Act
        orcamento.aprovar();

        // Assert
        assertEquals(2, orcamento.getHistorico().size()); // 1 criação + 1 aprovação
        HistoricoStatus hist = orcamento.getHistorico().get(1); // Última transição
        assertNotNull(hist.getDataTransicao());
        assertEquals(StatusOrcamento.PENDENTE, hist.getStatusAnterior());
        assertEquals(StatusOrcamento.APROVADO, hist.getStatusNovo());
    }

    @Test
    @DisplayName("Deve validar osId obrigatório")
    void deveValidarOsIdObrigatorio() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> Orcamento.criar(null, "Descrição"));
    }

    @Test
    @DisplayName("Deve validar descrição obrigatória")
    void deveValidarDescricaoObrigatoria() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> Orcamento.criar(UUID.randomUUID(), (String) null));

        assertThrows(IllegalArgumentException.class,
                () -> Orcamento.criar(UUID.randomUUID(), ""));
    }
}
