package br.com.grupo99.billingservice.application.mapper;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.domain.model.ItemOrcamento;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para converter entre Orcamento (domain) e DTOs
 * 
 * ✅ CLEAN ARCHITECTURE: Mapper isolado na application layer
 */
@Component
public class OrcamentoMapper {

    /**
     * Converte CreateOrcamentoRequest (DTO) → Orcamento (domain)
     */
    public Orcamento toDomain(CreateOrcamentoRequest request) {
        if (request == null) {
            return null;
        }

        List<ItemOrcamento> itens = new ArrayList<>();
        if (request.getItens() != null) {
            request.getItens().forEach(itemReq -> itens.add(ItemOrcamento.builder()
                    .descricao(itemReq.getDescricao())
                    .valorUnitario(itemReq.getValor())
                    .valorTotal(itemReq.getValor().multiply(new BigDecimal(itemReq.getQuantidade())))
                    .quantidade(itemReq.getQuantidade())
                    .build()));
        }

        Orcamento orcamento = Orcamento.criar(request.getOsId(), itens);
        if (request.getObservacao() != null) {
            orcamento.setObservacao(request.getObservacao());
        }

        return orcamento;
    }

    /**
     * Converte Orcamento (domain) → OrcamentoResponse (DTO)
     */
    public OrcamentoResponse toResponse(Orcamento orcamento) {
        if (orcamento == null) {
            return null;
        }

        List<OrcamentoResponse.ItemOrcamentoResponse> itensResponse = new ArrayList<>();
        if (orcamento.getItens() != null) {
            orcamento.getItens().forEach(item -> itensResponse.add(OrcamentoResponse.ItemOrcamentoResponse.builder()
                    .descricao(item.getDescricao())
                    .valor(item.getValorUnitario())
                    .quantidade(item.getQuantidade())
                    .build()));
        }

        return OrcamentoResponse.builder()
                .id(orcamento.getId())
                .osId(orcamento.getOsId())
                .status(orcamento.getStatus().name())
                .valorTotal(orcamento.getValorTotal())
                .itens(itensResponse)
                .observacao(orcamento.getObservacao())
                .dataGeracao(orcamento.getDataGeracao())
                .dataAprovacao(orcamento.getDataAprovacao())
                .dataRejeicao(orcamento.getDataRejeicao())
                .historicoCount(orcamento.getHistorico().size())
                .build();
    }
}
