package br.com.grupo99.billingservice.infrastructure.gateway;

import br.com.grupo99.billingservice.domain.gateway.MercadoPagoPort;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentPointOfInteraction;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter que implementa MercadoPagoPort usando o SDK oficial do Mercado Pago.
 *
 * ✅ CLEAN ARCHITECTURE: Implementação de infraestrutura do Port de domínio.
 * ✅ PREFERENCE API: Gera link de pagamento para o cliente pagar.
 * ✅ PAYMENT SEARCH: Checa status de pagamento por external_reference.
 */
@Slf4j
@Component
public class MercadoPagoAdapter implements MercadoPagoPort {

    private final PaymentClient paymentClient;
    private final PreferenceClient preferenceClient;

    @org.springframework.beans.factory.annotation.Value("${mercadopago.access-token:}")
    private String accessToken;

    public MercadoPagoAdapter(PaymentClient paymentClient, PreferenceClient preferenceClient) {
        this.paymentClient = paymentClient;
        this.preferenceClient = preferenceClient;
    }

    @Override
    public MercadoPagoPreferenceResult criarPreferencia(String descricao, BigDecimal valor,
            String payerEmail, String externalReference) {
        log.info("Criando preferência no Mercado Pago: descricao={}, valor={}, ref={}",
                descricao, valor, externalReference);

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Mercado Pago access token não configurado. Simulando preferência.");
            return new MercadoPagoPreferenceResult(
                    "PREF_SIMULADA_123",
                    "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=PREF_SIMULADA_123",
                    "https://sandbox.mercadopago.com.br/checkout/v1/redirect?pref_id=PREF_SIMULADA_123");
        }

        try {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title(descricao)
                    .quantity(1)
                    .unitPrice(valor)
                    .currencyId("BRL")
                    .build();

            PreferencePayerRequest payerRequest = PreferencePayerRequest.builder()
                    .email(payerEmail)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://oficina-digital.com/pagamento/sucesso")
                    .failure("https://oficina-digital.com/pagamento/falha")
                    .pending("https://oficina-digital.com/pagamento/pendente")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .payer(payerRequest)
                    .externalReference(externalReference)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .build();

            Preference preference = preferenceClient.create(preferenceRequest);

            log.info("Preferência criada no Mercado Pago: id={}, initPoint={}",
                    preference.getId(), preference.getInitPoint());

            return new MercadoPagoPreferenceResult(
                    preference.getId(),
                    preference.getInitPoint(),
                    preference.getSandboxInitPoint());

        } catch (MPApiException e) {
            String apiContent = e.getApiResponse() != null ? e.getApiResponse().getContent() : "N/A";
            log.error("╔══════════════════════════════════════════════════════");
            log.error("║ ERRO API MERCADO PAGO - Criar Preferência");
            log.error("║ Status Code: {}", e.getStatusCode());
            log.error("║ Resposta API: {}", apiContent);
            log.error("║ Mensagem: {}", e.getMessage());
            log.error("╚══════════════════════════════════════════════════════");
            throw new RuntimeException(
                    String.format("Erro Mercado Pago (HTTP %d): %s", e.getStatusCode(), apiContent), e);
        } catch (MPException e) {
            log.error("╔══════════════════════════════════════════════════════");
            log.error("║ ERRO SDK MERCADO PAGO - Criar Preferência");
            log.error("║ Mensagem: {}", e.getMessage());
            log.error("╚══════════════════════════════════════════════════════");
            throw new RuntimeException("Erro SDK Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Override
    public MercadoPagoPaymentResult consultarPagamento(Long paymentId) {
        log.info("Consultando pagamento no Mercado Pago: id={}", paymentId);

        try {
            Payment payment = paymentClient.get(paymentId);

            log.info("Pagamento consultado: id={}, status={}", payment.getId(), payment.getStatus());

            return new MercadoPagoPaymentResult(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    null, null, null);

        } catch (MPApiException e) {
            log.error("Erro ao consultar pagamento no Mercado Pago: statusCode={}",
                    e.getStatusCode(), e);
            throw new RuntimeException("Erro ao consultar pagamento no Mercado Pago: " + e.getMessage(), e);
        } catch (MPException e) {
            log.error("Erro no SDK do Mercado Pago", e);
            throw new RuntimeException("Erro ao consultar pagamento no Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Override
    public MercadoPagoPaymentResult buscarPagamentoPorReferencia(String externalReference) {
        log.info("Buscando pagamentos no Mercado Pago por referência: {}", externalReference);

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Mercado Pago access token não configurado. Simulando busca.");
            return new MercadoPagoPaymentResult(
                    123456789L, "approved", "accredited", null, null, null);
        }

        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("external_reference", externalReference);

            MPSearchRequest searchRequest = MPSearchRequest.builder()
                    .offset(0)
                    .limit(10)
                    .filters(filters)
                    .build();

            var searchResult = paymentClient.search(searchRequest);

            if (searchResult.getResults() != null && !searchResult.getResults().isEmpty()) {
                // Pega o pagamento mais recente
                Payment payment = searchResult.getResults().get(0);
                log.info("Pagamento encontrado para referência {}: id={}, status={}",
                        externalReference, payment.getId(), payment.getStatus());

                String qrCode = null;
                String qrCodeBase64 = null;
                String ticketUrl = null;

                if (payment.getPointOfInteraction() != null
                        && payment.getPointOfInteraction().getTransactionData() != null) {
                    qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
                    qrCodeBase64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
                    ticketUrl = payment.getPointOfInteraction().getTransactionData().getTicketUrl();
                }

                return new MercadoPagoPaymentResult(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getStatusDetail(),
                        qrCode, qrCodeBase64, ticketUrl);
            }

            log.info("Nenhum pagamento encontrado para referência: {}", externalReference);
            return new MercadoPagoPaymentResult(null, "not_found", "Nenhum pagamento encontrado", null, null, null);

        } catch (MPApiException e) {
            log.error("Erro ao buscar pagamentos no Mercado Pago: statusCode={}",
                    e.getStatusCode(), e);
            throw new RuntimeException("Erro ao buscar pagamentos no Mercado Pago: " + e.getMessage(), e);
        } catch (MPException e) {
            log.error("Erro no SDK do Mercado Pago", e);
            throw new RuntimeException("Erro ao buscar pagamentos no Mercado Pago: " + e.getMessage(), e);
        }
    }
}
