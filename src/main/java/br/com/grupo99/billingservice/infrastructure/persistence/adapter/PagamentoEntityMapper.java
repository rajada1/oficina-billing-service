package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.FormaPagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.PagamentoEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper para converter entre Pagamento (domain) e PagamentoEntity (DynamoDB)
 * 
 * ✅ CLEAN ARCHITECTURE: Mapper na infrastructure layer
 */
@Component
public class PagamentoEntityMapper {

    /**
     * Converte Domain Model → Entity
     */
    public PagamentoEntity toEntity(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }

        return PagamentoEntity.builder()
                .id(pagamento.getId() != null ? pagamento.getId().toString() : null)
                .orcamentoId(pagamento.getOrcamentoId() != null ? pagamento.getOrcamentoId().toString() : null)
                .osId(pagamento.getOsId() != null ? pagamento.getOsId().toString() : null)
                .status(pagamento.getStatus() != null ? pagamento.getStatus().name() : "PENDENTE")
                .valor(pagamento.getValor())
                .formaPagamento(pagamento.getFormaPagamento() != null ? pagamento.getFormaPagamento().name() : null)
                .comprovante(pagamento.getComprovante())
                .mercadoPagoPaymentId(pagamento.getMercadoPagoPaymentId())
                .mercadoPagoPreferenceId(pagamento.getMercadoPagoPreferenceId())
                .initPoint(pagamento.getInitPoint())
                .dataPagamento(pagamento.getDataPagamento())
                .dataEstorno(pagamento.getDataEstorno())
                .motivoEstorno(pagamento.getMotivoEstorno())
                .build();
    }

    /**
     * Converte Entity → Domain Model
     */
    public Pagamento toDomain(PagamentoEntity entity) {
        if (entity == null) {
            return null;
        }

        return Pagamento.builder()
                .id(entity.getId() != null ? UUID.fromString(entity.getId()) : null)
                .orcamentoId(entity.getOrcamentoId() != null ? UUID.fromString(entity.getOrcamentoId()) : null)
                .osId(entity.getOsId() != null ? UUID.fromString(entity.getOsId()) : null)
                .status(entity.getStatus() != null ? StatusPagamento.valueOf(entity.getStatus())
                        : StatusPagamento.PENDENTE)
                .valor(entity.getValor())
                .formaPagamento(
                        entity.getFormaPagamento() != null ? FormaPagamento.valueOf(entity.getFormaPagamento()) : null)
                .comprovante(entity.getComprovante())
                .mercadoPagoPaymentId(entity.getMercadoPagoPaymentId())
                .mercadoPagoPreferenceId(entity.getMercadoPagoPreferenceId())
                .initPoint(entity.getInitPoint())
                .dataPagamento(entity.getDataPagamento())
                .dataEstorno(entity.getDataEstorno())
                .motivoEstorno(entity.getMotivoEstorno())
                .build();
    }
}
