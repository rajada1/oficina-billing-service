package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pagamento - Testes Unitários")
class PagamentoTest {

        @Test
        @DisplayName("Deve criar pagamento com status PENDENTE")
        void deveCriarPagamentoComStatusPendente() {
                // Arrange
                UUID orcamentoId = UUID.randomUUID();
                UUID osId = UUID.randomUUID();
                BigDecimal valor = new BigDecimal("500.00");

                // Act
                Pagamento pagamento = Pagamento.criar(orcamentoId, osId, valor, FormaPagamento.CARTAO_CREDITO);

                // Assert
                assertNotNull(pagamento);
                assertNotNull(pagamento.getId());
                assertEquals(orcamentoId, pagamento.getOrcamentoId());
                assertEquals(osId, pagamento.getOsId());
                assertEquals(valor, pagamento.getValor());
                assertEquals(FormaPagamento.CARTAO_CREDITO, pagamento.getFormaPagamento());
                assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
                // createdAt não é preenchido em testes unitários (precisa de contexto Spring
                // com auditing)
                assertNull(pagamento.getDataConfirmacao());
        }

        @Test
        @DisplayName("Deve confirmar pagamento PENDENTE")
        void deveConfirmarPagamentoPendente() {
                // Arrange
                Pagamento pagamento = Pagamento.criar(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new BigDecimal("300.00"),
                                FormaPagamento.PIX);

                // Act
                pagamento.confirmar();

                // Assert
                assertEquals(StatusPagamento.CONFIRMADO, pagamento.getStatus());
                assertNotNull(pagamento.getDataConfirmacao());
        }

        @Test
        @DisplayName("Deve estornar pagamento CONFIRMADO")
        void deveEstornarPagamentoConfirmado() {
                // Arrange
                Pagamento pagamento = Pagamento.criar(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new BigDecimal("450.00"),
                                FormaPagamento.CARTAO_DEBITO);
                pagamento.confirmar();

                // Act
                pagamento.estornar();

                // Assert
                assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        }

        @Test
        @DisplayName("Deve ser idempotente ao confirmar pagamento já confirmado")
        void deveSerIdempotenteAoConfirmarPagamentoJaConfirmado() {
                // Arrange
                Pagamento pagamento = Pagamento.criar(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new BigDecimal("200.00"),
                                FormaPagamento.DINHEIRO);
                pagamento.confirmar();

                // Act
                pagamento.confirmar();

                // Assert
                assertEquals(StatusPagamento.CONFIRMADO, pagamento.getStatus());
        }

        @Test
        @DisplayName("Não deve estornar pagamento PENDENTE")
        void naoDeveEstornarPagamentoPendente() {
                // Arrange
                Pagamento pagamento = Pagamento.criar(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new BigDecimal("150.00"),
                                FormaPagamento.BOLETO);

                // Act & Assert
                assertThrows(IllegalStateException.class, () -> pagamento.estornar());
        }

        @Test
        @DisplayName("Deve ser idempotente ao estornar pagamento já estornado")
        void deveSerIdempotenteAoEstornarPagamentoJaEstornado() {
                // Arrange
                Pagamento pagamento = Pagamento.criar(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new BigDecimal("600.00"),
                                FormaPagamento.TRANSFERENCIA);
                pagamento.confirmar();
                pagamento.estornar();

                // Act
                pagamento.estornar();

                // Assert
                assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        }

        @Test
        @DisplayName("Deve validar orcamentoId obrigatório")
        void deveValidarOrcamentoIdObrigatorio() {
                // Act & Assert
                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(null, UUID.randomUUID(), new BigDecimal("100.00"),
                                                FormaPagamento.PIX));
        }

        @Test
        @DisplayName("Deve validar osId obrigatório")
        void deveValidarOsIdObrigatorio() {
                // Act & Assert
                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(UUID.randomUUID(), null, new BigDecimal("100.00"),
                                                FormaPagamento.PIX));
        }

        @Test
        @DisplayName("Deve validar valor obrigatório e positivo")
        void deveValidarValorObrigatorioEPositivo() {
                // Act & Assert
                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), null, FormaPagamento.PIX));

                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO,
                                                FormaPagamento.PIX));

                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-100.00"),
                                                FormaPagamento.PIX));
        }

        @Test
        @DisplayName("Deve validar forma de pagamento obrigatória")
        void deveValidarFormaPagamentoObrigatoria() {
                // Act & Assert
                assertThrows(IllegalArgumentException.class,
                                () -> Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                                null));
        }

        @Test
        @DisplayName("Deve aceitar todas as formas de pagamento")
        void deveAceitarTodasAsFormasDePagamento() {
                // Arrange & Act
                Pagamento pag1 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.DINHEIRO);
                Pagamento pag2 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.CARTAO_CREDITO);
                Pagamento pag3 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.CARTAO_DEBITO);
                Pagamento pag4 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.PIX);
                Pagamento pag5 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.TRANSFERENCIA);
                Pagamento pag6 = Pagamento.criar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                                FormaPagamento.BOLETO);

                // Assert
                assertNotNull(pag1);
                assertNotNull(pag2);
                assertNotNull(pag3);
                assertNotNull(pag4);
                assertNotNull(pag5);
                assertNotNull(pag6);
        }
}
