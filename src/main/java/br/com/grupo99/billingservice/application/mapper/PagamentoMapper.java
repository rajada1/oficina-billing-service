package br.com.grupo99.billingservice.application.mapper;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.domain.model.FormaPagamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Mapper para converter entre Pagamento (domain) e DTOs
 * 
 * ✅ CLEAN ARCHITECTURE: Mapper isolado na application layer
 */
@Component
public class PagamentoMapper {

    /**
     * Converte CreatePagamentoRequest (DTO) → Pagamento (domain)
     */
    public Pagamento toDomain(CreatePagamentoRequest request) {
        if (request == null) {
            return null;
        }

        FormaPagamento formaPagamento = FormaPagamento.valueOf(
                request.getFormaPagamento().toUpperCase());

        return Pagamento.criar(
                request.getOrcamentoId(),
                request.getOsId(),
                request.getValor(),
                formaPagamento,
                request.getComprovante());
    }

    /**
     * Converte Pagamento (domain) → PagamentoResponse (DTO)
     */
    public PagamentoResponse toResponse(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }

        return PagamentoResponse.builder()
                .id(pagamento.getId())
                .orcamentoId(pagamento.getOrcamentoId())
                .osId(pagamento.getOsId())
                .status(pagamento.getStatus().name())
                .valor(pagamento.getValor())
                .formaPagamento(pagamento.getFormaPagamento().name())
                .comprovante(pagamento.getComprovante())
                .mercadoPagoPaymentId(pagamento.getMercadoPagoPaymentId())
                .mercadoPagoPreferenceId(pagamento.getMercadoPagoPreferenceId())
                .initPoint(pagamento.getInitPoint())
                .dataPagamento(pagamento.getDataPagamento())
                .dataEstorno(pagamento.getDataEstorno())
                .motivoEstorno(pagamento.getMotivoEstorno())
                .build();
    }
}
