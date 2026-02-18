package br.com.grupo99.billingservice.application.service;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.application.mapper.PagamentoMapper;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPaymentResult;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPreferenceResult;
import br.com.grupo99.billingservice.domain.model.FormaPagamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import br.com.grupo99.billingservice.domain.repository.PagamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoApplicationService - Testes unitários")
class PagamentoApplicationServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private BillingEventPublisherPort eventPublisher;

    @Mock
    private MercadoPagoPort mercadoPagoPort;

    @Mock
    private PagamentoMapper mapper;

    @InjectMocks
    private PagamentoApplicationService service;

    private UUID orcamentoId;
    private UUID osId;
    private UUID pagamentoId;
    private Pagamento pagamento;
    private PagamentoResponse pagamentoResponse;
    private CreatePagamentoRequest createRequest;

    @BeforeEach
    void setUp() {
        orcamentoId = UUID.randomUUID();
        osId = UUID.randomUUID();
        pagamentoId = UUID.randomUUID();

        pagamento = Pagamento.builder()
                .id(pagamentoId)
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status(StatusPagamento.PENDENTE)
                .valor(new BigDecimal("500.00"))
                .formaPagamento(FormaPagamento.PIX)
                .build();

        pagamentoResponse = PagamentoResponse.builder()
                .id(pagamentoId)
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status("PENDENTE")
                .valor(new BigDecimal("500.00"))
                .formaPagamento("PIX")
                .build();

        createRequest = CreatePagamentoRequest.builder()
                .orcamentoId(orcamentoId)
                .osId(osId)
                .valor(new BigDecimal("500.00"))
                .formaPagamento("PIX")
                .payerEmail("test@test.com")
                .build();
    }

    @Nested
    @DisplayName("Registrar Pagamento")
    class RegistrarPagamento {

        @Test
        @DisplayName("Deve registrar pagamento com sucesso")
        void deveRegistrarPagamentoComSucesso() {
            MercadoPagoPreferenceResult prefResult = new MercadoPagoPreferenceResult(
                    "pref-123", "https://mp.com/pay", "https://sandbox.mp.com/pay");

            when(mapper.toDomain(createRequest)).thenReturn(pagamento);
            when(mercadoPagoPort.criarPreferencia(anyString(), any(BigDecimal.class), anyString(), anyString()))
                    .thenReturn(prefResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.registrar(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(pagamentoId);
            verify(mercadoPagoPort).criarPreferencia(anyString(), any(BigDecimal.class), anyString(), anyString());
            verify(pagamentoRepository).save(any(Pagamento.class));
            verify(eventPublisher).publicarPagamentoRegistrado(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve usar email padrão quando payerEmail é null")
        void deveUsarEmailPadraoQuandoPayerEmailNull() {
            CreatePagamentoRequest requestSemEmail = CreatePagamentoRequest.builder()
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .valor(new BigDecimal("500.00"))
                    .formaPagamento("PIX")
                    .payerEmail(null)
                    .build();

            MercadoPagoPreferenceResult prefResult = new MercadoPagoPreferenceResult(
                    "pref-123", "https://mp.com/pay", null);

            when(mapper.toDomain(requestSemEmail)).thenReturn(pagamento);
            when(mercadoPagoPort.criarPreferencia(anyString(), any(BigDecimal.class), eq("test@test.com"), anyString()))
                    .thenReturn(prefResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.registrar(requestSemEmail);

            assertThat(result).isNotNull();
            verify(mercadoPagoPort).criarPreferencia(anyString(), any(BigDecimal.class), eq("test@test.com"), anyString());
        }
    }

    @Nested
    @DisplayName("Checar Pagamento")
    class ChecarPagamento {

        @Test
        @DisplayName("Deve retornar direto se pagamento já está em estado final")
        void deveRetornarDiretoSeEstadoFinal() {
            Pagamento pagConfirmado = Pagamento.builder()
                    .id(pagamentoId)
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .status(StatusPagamento.CONFIRMADO)
                    .valor(new BigDecimal("500.00"))
                    .formaPagamento(FormaPagamento.PIX)
                    .build();

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagConfirmado));
            when(mapper.toResponse(pagConfirmado)).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verifyNoInteractions(mercadoPagoPort);
        }

        @Test
        @DisplayName("Deve confirmar pagamento quando MP retorna approved")
        void deveConfirmarQuandoMPApproved() {
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    12345L, "approved", "accredited", null, null, null);

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(mercadoPagoPort.buscarPagamentoPorReferencia(pagamentoId.toString())).thenReturn(mpResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verify(pagamentoRepository).save(any(Pagamento.class));
            verify(eventPublisher).publicarPagamentoConfirmado(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve processar pagamento quando MP retorna pending")
        void deveProcessarQuandoMPPending() {
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    12345L, "pending", "pending", null, null, null);

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(mercadoPagoPort.buscarPagamentoPorReferencia(pagamentoId.toString())).thenReturn(mpResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verify(pagamentoRepository).save(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve cancelar pagamento quando MP retorna rejected")
        void deveCancelarQuandoMPRejected() {
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    12345L, "rejected", "cc_rejected", null, null, null);

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(mercadoPagoPort.buscarPagamentoPorReferencia(pagamentoId.toString())).thenReturn(mpResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verify(pagamentoRepository).save(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve estornar pagamento quando MP retorna refunded")
        void deveEstornarQuandoMPRefunded() {
            Pagamento pagConfirmado = Pagamento.builder()
                    .id(pagamentoId)
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .status(StatusPagamento.PROCESSANDO)
                    .valor(new BigDecimal("500.00"))
                    .formaPagamento(FormaPagamento.PIX)
                    .build();

            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    12345L, "refunded", "refunded", null, null, null);

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagConfirmado));
            when(mercadoPagoPort.buscarPagamentoPorReferencia(pagamentoId.toString())).thenReturn(mpResult);
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagConfirmado);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verify(eventPublisher).publicarPagamentoEstornado(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve lidar com not_found no MP")
        void deveManterPendenteQuandoNotFound() {
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    null, null, null, null, null, null);

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(mercadoPagoPort.buscarPagamentoPorReferencia(pagamentoId.toString())).thenReturn(mpResult);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.checarPagamento(pagamentoId);

            assertThat(result).isNotNull();
            verify(pagamentoRepository, never()).save(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando pagamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checarPagamento(pagamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pagamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Processar Webhook")
    class ProcessarWebhook {

        @Test
        @DisplayName("Deve confirmar pagamento via webhook quando approved")
        void deveConfirmarPagamentoViaWebhook() {
            Long mpPaymentId = 12345L;
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    mpPaymentId, "approved", "accredited", null, null, null);

            when(mercadoPagoPort.consultarPagamento(mpPaymentId)).thenReturn(mpResult);
            when(pagamentoRepository.findByMercadoPagoPaymentId(mpPaymentId)).thenReturn(Optional.of(pagamento));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

            service.processarWebhook(mpPaymentId);

            verify(pagamentoRepository).save(any(Pagamento.class));
            verify(eventPublisher).publicarPagamentoConfirmado(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve cancelar pagamento via webhook quando rejected")
        void deveCancelarPagamentoViaWebhook() {
            Long mpPaymentId = 12345L;
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    mpPaymentId, "rejected", "cc_rejected", null, null, null);

            when(mercadoPagoPort.consultarPagamento(mpPaymentId)).thenReturn(mpResult);
            when(pagamentoRepository.findByMercadoPagoPaymentId(mpPaymentId)).thenReturn(Optional.of(pagamento));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

            service.processarWebhook(mpPaymentId);

            verify(pagamentoRepository).save(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando pagamento não encontrado por MP ID")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            Long mpPaymentId = 12345L;
            MercadoPagoPaymentResult mpResult = new MercadoPagoPaymentResult(
                    mpPaymentId, "approved", "accredited", null, null, null);

            when(mercadoPagoPort.consultarPagamento(mpPaymentId)).thenReturn(mpResult);
            when(pagamentoRepository.findByMercadoPagoPaymentId(mpPaymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processarWebhook(mpPaymentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pagamento não encontrado para MP ID");
        }
    }

    @Nested
    @DisplayName("Confirmar Pagamento")
    class ConfirmarPagamento {

        @Test
        @DisplayName("Deve confirmar pagamento manualmente com sucesso")
        void deveConfirmarPagamentoComSucesso() {
            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.confirmar(pagamentoId);

            assertThat(result).isNotNull();
            verify(eventPublisher).publicarPagamentoConfirmado(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmar(pagamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pagamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Estornar Pagamento")
    class EstornarPagamento {

        @Test
        @DisplayName("Deve estornar pagamento com sucesso")
        void deveEstornarPagamentoComSucesso() {
            Pagamento pagConfirmado = Pagamento.builder()
                    .id(pagamentoId)
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .status(StatusPagamento.CONFIRMADO)
                    .valor(new BigDecimal("500.00"))
                    .formaPagamento(FormaPagamento.PIX)
                    .build();

            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagConfirmado));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagConfirmado);
            when(mapper.toResponse(any(Pagamento.class))).thenReturn(pagamentoResponse);

            PagamentoResponse result = service.estornar(pagamentoId, "Erro no serviço");

            assertThat(result).isNotNull();
            verify(eventPublisher).publicarPagamentoEstornado(any(Pagamento.class));
        }
    }

    @Nested
    @DisplayName("Cancelar Pagamento")
    class CancelarPagamento {

        @Test
        @DisplayName("Deve cancelar pagamento com sucesso")
        void deveCancelarPagamentoComSucesso() {
            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

            service.cancelar(pagamentoId);

            verify(pagamentoRepository).save(any(Pagamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelar(pagamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pagamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Listar Todos")
    class ListarTodos {

        @Test
        @DisplayName("Deve listar todos os pagamentos")
        void deveListarTodosOsPagamentos() {
            when(pagamentoRepository.findAll()).thenReturn(List.of(pagamento));
            when(mapper.toResponse(pagamento)).thenReturn(pagamentoResponse);

            List<PagamentoResponse> result = service.listarTodos();

            assertThat(result).hasSize(1);
            verify(pagamentoRepository).findAll();
        }
    }
}
