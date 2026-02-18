package br.com.grupo99.billingservice.domain.gateway;

import java.math.BigDecimal;

/**
 * Port (interface) para integração com gateway de pagamento externo.
 *
 * ✅ CLEAN ARCHITECTURE: Interface no domínio, implementação na infraestrutura.
 */
public interface MercadoPagoPort {

    /**
     * Cria uma preferência de pagamento no Mercado Pago (gera link de pagamento).
     *
     * @param descricao      descrição do pagamento (ex: "Pagamento OS #123")
     * @param valor          valor do pagamento
     * @param payerEmail     email do pagador
     * @param externalReference referência externa (ex: pagamentoId)
     * @return resultado com link de pagamento e ID da preferência
     */
    MercadoPagoPreferenceResult criarPreferencia(String descricao, BigDecimal valor,
            String payerEmail, String externalReference);

    /**
     * Consulta o status de um pagamento no Mercado Pago pelo ID do pagamento.
     *
     * @param paymentId ID do pagamento no MP
     * @return resultado com status atualizado
     */
    MercadoPagoPaymentResult consultarPagamento(Long paymentId);

    /**
     * Busca pagamentos associados a uma preferência (por external_reference).
     *
     * @param externalReference referência externa usada na criação da preferência
     * @return resultado com status do pagamento encontrado (ou null se nenhum)
     */
    MercadoPagoPaymentResult buscarPagamentoPorReferencia(String externalReference);

    /**
     * Resultado da criação de uma preferência no Mercado Pago.
     */
    record MercadoPagoPreferenceResult(
            String preferenceId,
            String initPoint,
            String sandboxInitPoint) {
    }

    /**
     * Resultado de uma consulta de pagamento no Mercado Pago.
     */
    record MercadoPagoPaymentResult(
            Long paymentId,
            String status,
            String statusDetail,
            String qrCode,
            String qrCodeBase64,
            String ticketUrl) {
    }
}
