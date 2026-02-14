package br.com.grupo99.billingservice.domain.model;

import lombok.Getter;

/**
 * Enum representando os tipos de item em um orçamento.
 */
@Getter
public enum TipoItem {

    SERVICO("Serviço"),
    PECA("Peça"),
    DIAGNOSTICO("Diagnóstico"),
    MAO_DE_OBRA("Mão de Obra");

    private final String descricao;

    TipoItem(String descricao) {
        this.descricao = descricao;
    }
}
