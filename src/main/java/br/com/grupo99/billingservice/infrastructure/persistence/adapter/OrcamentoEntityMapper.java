package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.HistoricoStatus;
import br.com.grupo99.billingservice.domain.model.ItemOrcamento;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.OrcamentoEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapper para converter entre Orcamento (domain) e OrcamentoEntity (MongoDB)
 * 
 * ✅ CLEAN ARCHITECTURE: Mapper na infrastructure layer
 * Converte: Domain Model ↔ Persistence Entity
 */
@Component
public class OrcamentoEntityMapper {

    /**
     * Converte Domain Model → Entity (para persistência)
     */
    public OrcamentoEntity toEntity(Orcamento orcamento) {
        if (orcamento == null) {
            return null;
        }

        // Converter itens
        List<OrcamentoEntity.ItemOrcamentoEntity> itensEntity = new ArrayList<>();
        if (orcamento.getItens() != null) {
            orcamento.getItens().forEach(item -> itensEntity.add(OrcamentoEntity.ItemOrcamentoEntity.builder()
                    .descricao(item.getDescricao())
                    .valorUnitario(item.getValorUnitario())
                    .valorTotal(item.getValorTotal())
                    .quantidade(item.getQuantidade())
                    .tipo(item.getTipo() != null ? item.getTipo().name() : null)
                    .build()));
        }

        // Converter histórico
        List<OrcamentoEntity.HistoricoStatusEntity> historicoEntity = new ArrayList<>();
        if (orcamento.getHistorico() != null) {
            orcamento.getHistorico().forEach(hist -> historicoEntity.add(OrcamentoEntity.HistoricoStatusEntity.builder()
                    .statusAnterior(hist.getStatusAnterior() != null ? hist.getStatusAnterior().name() : null)
                    .novoStatus(hist.getNovoStatus() != null ? hist.getNovoStatus().name() : null)
                    .usuario(hist.getUsuario())
                    .observacao(hist.getObservacao())
                    .data(hist.getData())
                    .build()));
        }

        return OrcamentoEntity.builder()
                .id(orcamento.getId() != null ? orcamento.getId().toString() : null)
                .osId(orcamento.getOsId() != null ? orcamento.getOsId().toString() : null)
                .status(orcamento.getStatus() != null ? orcamento.getStatus().name() : "PENDENTE")
                .itens(itensEntity)
                .valorTotal(orcamento.getValorTotal())
                .dataGeracao(orcamento.getDataGeracao())
                .dataAprovacao(orcamento.getDataAprovacao())
                .dataRejeicao(orcamento.getDataRejeicao())
                .observacao(orcamento.getObservacao())
                .motivoRejeicao(orcamento.getMotivoRejeicao())
                .historico(historicoEntity)
                .build();
    }

    /**
     * Converte Entity → Domain Model (após recuperar do BD)
     */
    public Orcamento toDomain(OrcamentoEntity entity) {
        if (entity == null) {
            return null;
        }

        // Converter itens
        List<ItemOrcamento> itens = new ArrayList<>();
        if (entity.getItens() != null) {
            entity.getItens().forEach(itemEntity -> itens.add(ItemOrcamento.builder()
                    .descricao(itemEntity.getDescricao())
                    .valorUnitario(itemEntity.getValorUnitario())
                    .valorTotal(itemEntity.getValorTotal())
                    .quantidade(itemEntity.getQuantidade())
                    .tipo(itemEntity.getTipo() != null ? Enum.valueOf(
                            br.com.grupo99.billingservice.domain.model.TipoItem.class, itemEntity.getTipo()) : null)
                    .build()));
        }

        // Converter histórico
        List<HistoricoStatus> historico = new ArrayList<>();
        if (entity.getHistorico() != null) {
            entity.getHistorico().forEach(histEntity -> historico.add(HistoricoStatus.builder()
                    .statusAnterior(histEntity.getStatusAnterior() != null
                            ? StatusOrcamento.valueOf(histEntity.getStatusAnterior())
                            : null)
                    .novoStatus(histEntity.getNovoStatus() != null ? StatusOrcamento.valueOf(histEntity.getNovoStatus())
                            : null)
                    .usuario(histEntity.getUsuario())
                    .observacao(histEntity.getObservacao())
                    .data(histEntity.getData())
                    .build()));
        }

        return Orcamento.builder()
                .id(entity.getId() != null ? UUID.fromString(entity.getId()) : null)
                .osId(entity.getOsId() != null ? UUID.fromString(entity.getOsId()) : null)
                .status(entity.getStatus() != null ? StatusOrcamento.valueOf(entity.getStatus())
                        : StatusOrcamento.PENDENTE)
                .itens(itens)
                .valorTotal(entity.getValorTotal())
                .dataGeracao(entity.getDataGeracao())
                .dataAprovacao(entity.getDataAprovacao())
                .dataRejeicao(entity.getDataRejeicao())
                .observacao(entity.getObservacao())
                .motivoRejeicao(entity.getMotivoRejeicao())
                .historico(historico)
                .build();
    }
}
