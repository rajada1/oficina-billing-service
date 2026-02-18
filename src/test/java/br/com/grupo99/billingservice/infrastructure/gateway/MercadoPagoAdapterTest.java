package br.com.grupo99.billingservice.infrastructure.gateway;

import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPaymentResult;
import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort.MercadoPagoPreferenceResult;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPagoAdapter - Testes unitários")
class MercadoPagoAdapterTest {

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private PreferenceClient preferenceClient;

    @InjectMocks
    private MercadoPagoAdapter adapter;

    @Nested
    @DisplayName("Criar Preferência")
    class CriarPreferencia {

        @Test
        @DisplayName("Deve simular preferência quando access token não configurado")
        void deveSimularPreferenciaQuandoSemToken() {
            ReflectionTestUtils.setField(adapter, "accessToken", "");

            MercadoPagoPreferenceResult result = adapter.criarPreferencia(
                    "Pagamento OS", new BigDecimal("500.00"), "test@test.com", "ref-123");

            assertThat(result).isNotNull();
            assertThat(result.preferenceId()).isEqualTo("PREF_SIMULADA_123");
            assertThat(result.initPoint()).contains("PREF_SIMULADA_123");
        }

        @Test
        @DisplayName("Deve simular preferência quando access token é null")
        void deveSimularPreferenciaQuandoTokenNull() {
            ReflectionTestUtils.setField(adapter, "accessToken", null);

            MercadoPagoPreferenceResult result = adapter.criarPreferencia(
                    "Pagamento OS", new BigDecimal("500.00"), "test@test.com", "ref-123");

            assertThat(result).isNotNull();
            assertThat(result.preferenceId()).isEqualTo("PREF_SIMULADA_123");
        }

        @Test
        @DisplayName("Deve criar preferência real quando token configurado")
        void deveCriarPreferenciaRealComToken() throws MPException, MPApiException {
            ReflectionTestUtils.setField(adapter, "accessToken", "TEST-TOKEN-123");

            Preference preference = mock(Preference.class);
            when(preference.getId()).thenReturn("pref-real-123");
            when(preference.getInitPoint()).thenReturn("https://mp.com/pay/real");
            when(preference.getSandboxInitPoint()).thenReturn("https://sandbox.mp.com/pay/real");

            when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(preference);

            MercadoPagoPreferenceResult result = adapter.criarPreferencia(
                    "Pagamento OS", new BigDecimal("500.00"), "test@test.com", "ref-123");

            assertThat(result).isNotNull();
            assertThat(result.preferenceId()).isEqualTo("pref-real-123");
            assertThat(result.initPoint()).isEqualTo("https://mp.com/pay/real");
            verify(preferenceClient).create(any(PreferenceRequest.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando MPApiException")
        void deveLancarExcecaoQuandoMPApiException() throws MPException, MPApiException {
            ReflectionTestUtils.setField(adapter, "accessToken", "TEST-TOKEN-123");

            MPApiException apiException = mock(MPApiException.class);
            when(apiException.getStatusCode()).thenReturn(400);
            when(apiException.getMessage()).thenReturn("Bad Request");
            when(apiException.getApiResponse()).thenReturn(null);

            when(preferenceClient.create(any(PreferenceRequest.class))).thenThrow(apiException);

            assertThatThrownBy(() -> adapter.criarPreferencia(
                    "Pagamento OS", new BigDecimal("500.00"), "test@test.com", "ref-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro Mercado Pago");
        }

        @Test
        @DisplayName("Deve lançar exceção quando MPException")
        void deveLancarExcecaoQuandoMPException() throws MPException, MPApiException {
            ReflectionTestUtils.setField(adapter, "accessToken", "TEST-TOKEN-123");

            when(preferenceClient.create(any(PreferenceRequest.class)))
                    .thenThrow(new MPException("SDK Error"));

            assertThatThrownBy(() -> adapter.criarPreferencia(
                    "Pagamento OS", new BigDecimal("500.00"), "test@test.com", "ref-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro SDK Mercado Pago");
        }
    }

    @Nested
    @DisplayName("Consultar Pagamento")
    class ConsultarPagamento {

        @Test
        @DisplayName("Deve consultar pagamento com sucesso")
        void deveConsultarPagamentoComSucesso() throws MPException, MPApiException {
            Payment payment = mock(Payment.class);
            when(payment.getId()).thenReturn(12345L);
            when(payment.getStatus()).thenReturn("approved");
            when(payment.getStatusDetail()).thenReturn("accredited");

            when(paymentClient.get(12345L)).thenReturn(payment);

            MercadoPagoPaymentResult result = adapter.consultarPagamento(12345L);

            assertThat(result).isNotNull();
            assertThat(result.paymentId()).isEqualTo(12345L);
            assertThat(result.status()).isEqualTo("approved");
            assertThat(result.statusDetail()).isEqualTo("accredited");
        }

        @Test
        @DisplayName("Deve lançar exceção quando MPApiException na consulta")
        void deveLancarExcecaoQuandoMPApiException() throws MPException, MPApiException {
            MPApiException apiException = mock(MPApiException.class);
            when(apiException.getStatusCode()).thenReturn(404);
            when(apiException.getMessage()).thenReturn("Not Found");

            when(paymentClient.get(12345L)).thenThrow(apiException);

            assertThatThrownBy(() -> adapter.consultarPagamento(12345L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao consultar pagamento");
        }

        @Test
        @DisplayName("Deve lançar exceção quando MPException na consulta")
        void deveLancarExcecaoQuandoMPException() throws MPException, MPApiException {
            when(paymentClient.get(12345L)).thenThrow(new MPException("SDK Error"));

            assertThatThrownBy(() -> adapter.consultarPagamento(12345L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao consultar pagamento");
        }
    }

    @Nested
    @DisplayName("Buscar Pagamento por Referência")
    class BuscarPorReferencia {

        @Test
        @DisplayName("Deve simular busca quando access token não configurado")
        void deveSimularBuscaQuandoSemToken() {
            ReflectionTestUtils.setField(adapter, "accessToken", "");

            MercadoPagoPaymentResult result = adapter.buscarPagamentoPorReferencia("ref-123");

            assertThat(result).isNotNull();
            assertThat(result.paymentId()).isEqualTo(123456789L);
            assertThat(result.status()).isEqualTo("approved");
        }

        @Test
        @DisplayName("Deve retornar not_found quando nenhum resultado encontrado")
        @SuppressWarnings("unchecked")
        void deveRetornarNotFoundQuandoSemResultados() throws MPException, MPApiException {
            ReflectionTestUtils.setField(adapter, "accessToken", "TEST-TOKEN-123");

            com.mercadopago.net.MPResultsResourcesPage<Payment> searchResult =
                    mock(com.mercadopago.net.MPResultsResourcesPage.class);
            when(searchResult.getResults()).thenReturn(java.util.List.of());

            when(paymentClient.search(any(MPSearchRequest.class))).thenReturn(searchResult);

            MercadoPagoPaymentResult result = adapter.buscarPagamentoPorReferencia("ref-123");

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("not_found");
        }

        @Test
        @DisplayName("Deve lançar exceção quando MPApiException na busca")
        void deveLancarExcecaoQuandoMPApiException() throws MPException, MPApiException {
            ReflectionTestUtils.setField(adapter, "accessToken", "TEST-TOKEN-123");

            MPApiException apiException = mock(MPApiException.class);
            when(apiException.getStatusCode()).thenReturn(500);
            when(apiException.getMessage()).thenReturn("Internal Error");

            when(paymentClient.search(any(MPSearchRequest.class))).thenThrow(apiException);

            assertThatThrownBy(() -> adapter.buscarPagamentoPorReferencia("ref-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao buscar pagamentos");
        }
    }
}
