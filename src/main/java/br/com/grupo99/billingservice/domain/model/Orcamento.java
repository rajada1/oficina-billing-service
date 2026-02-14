package br.com.grupo99.billingservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root representando um Orçamento.
 * Gerencia itens, aprovações, rejeições e histórico.
 * 
 * ✅ CLEAN ARCHITECTURE: Domain sem dependências externas (sem Spring Data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orcamento {

    private UUID id;

    private UUID osId; // ID da Ordem de Serviço

    @Builder.Default
    private StatusOrcamento status = StatusOrcamento.PENDENTE;

    @Builder.Default
    private List<ItemOrcamento> itens = new ArrayList<>();

    private BigDecimal valorTotal;

    private Instant dataGeracao;

    private Instant dataAprovacao;

    private Instant dataRejeicao;

    private String observacao;

    private String motivoRejeicao;

    @Builder.Default
    private List<HistoricoStatus> historico = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Factory method para criar novo orçamento.
     *
     * @param osId  ID da OS
     * @param itens lista de itens
     * @return novo Orcamento
     */
    public static Orcamento criar(UUID osId, List<ItemOrcamento> itens) {
        Orcamento orcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osId)
                .status(StatusOrcamento.PENDENTE)
                .itens(itens)
                .dataGeracao(Instant.now())
                .historico(new ArrayList<>())
                .build();

        orcamento.calcularValorTotal();
        orcamento.adicionarHistorico(null, StatusOrcamento.PENDENTE, "system", "Orçamento gerado");

        return orcamento;
    }

    /**
     * Factory method sobrecargado para criar com descrição (compatibilidade com
     * testes)
     */
    public static Orcamento criar(UUID osId, String descricao) {
        if (osId == null) {
            throw new IllegalArgumentException("OS ID não pode ser nulo");
        }
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }

        Orcamento orcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osId)
                .status(StatusOrcamento.PENDENTE)
                .itens(new ArrayList<>())
                .dataGeracao(Instant.now())
                .historico(new ArrayList<>())
                .valorTotal(BigDecimal.ZERO)
                .observacao(descricao)
                .build();

        orcamento.adicionarHistorico(null, StatusOrcamento.PENDENTE, "system", "Orçamento gerado");

        return orcamento;
    }

    /**
     * Adiciona um item ao orçamento
     */
    public void adicionarItem(ItemOrcamento item) {
        if (item == null) {
            throw new IllegalArgumentException("Item não pode ser nulo");
        }
        this.itens.add(item);
        calcularValorTotal();
    }

    /**
     * Retorna o osId.
     */
    public UUID getOsIdValue() {
        return this.osId;
    }

    /**
     * Calcula o valor total somando todos os itens.
     */
    public void calcularValorTotal() {
        this.valorTotal = itens.stream()
                .map(ItemOrcamento::calcularValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Aprova o orçamento.
     *
     * @param usuario    usuário que aprovou
     * @param observacao observação opcional
     * @throws IllegalStateException se transição não for válida
     */
    public void aprovar(String usuario, String observacao) {
        validarTransicao(StatusOrcamento.APROVADO);

        StatusOrcamento statusAnterior = this.status;
        this.status = StatusOrcamento.APROVADO;
        this.dataAprovacao = Instant.now();
        this.observacao = observacao;

        adicionarHistorico(statusAnterior, StatusOrcamento.APROVADO, usuario, observacao);
    }

    /**
     * Sobrecarga sem parâmetros para testes
     */
    public void aprovar() {
        aprovar("Sistema", "Orçamento aprovado");
    }

    /**
     * Rejeita o orçamento.
     *
     * @param usuario usuário que rejeitou
     * @param motivo  motivo da rejeição
     * @throws IllegalStateException se transição não for válida
     */
    public void rejeitar(String usuario, String motivo) {
        validarTransicao(StatusOrcamento.REJEITADO);

        StatusOrcamento statusAnterior = this.status;
        this.status = StatusOrcamento.REJEITADO;
        this.dataRejeicao = Instant.now();
        this.motivoRejeicao = motivo;

        adicionarHistorico(statusAnterior, StatusOrcamento.REJEITADO, usuario, motivo);
    }

    /**
     * Sobrecarga sem parâmetros para testes
     */
    public void rejeitar() {
        rejeitar("Sistema", "Orçamento rejeitado");
    }

    /**
     * Cancela o orçamento.
     *
     * @param usuario usuário que cancelou
     * @param motivo  motivo do cancelamento
     * @throws IllegalStateException se transição não for válida
     */
    public void cancelar(String usuario, String motivo) {
        validarTransicao(StatusOrcamento.CANCELADO);

        StatusOrcamento statusAnterior = this.status;
        this.status = StatusOrcamento.CANCELADO;

        adicionarHistorico(statusAnterior, StatusOrcamento.CANCELADO, usuario, motivo);
    }

    /**
     * Sobrecarga sem parâmetros para testes
     */
    public void cancelar() {
        cancelar("Sistema", "Orçamento cancelado");
    }

    /**
     * Valida se a transição de status é permitida.
     *
     * @param novoStatus novo status desejado
     * @throws IllegalStateException se transição não for válida
     */
    private void validarTransicao(StatusOrcamento novoStatus) {
        if (!this.status.podeTransicionarPara(novoStatus)) {
            throw new IllegalStateException(
                    String.format("Transição inválida de %s para %s", this.status, novoStatus));
        }
    }

    /**
     * Adiciona entrada no histórico.
     *
     * @param statusAnterior status anterior
     * @param novoStatus     novo status
     * @param usuario        usuário
     * @param observacao     observação
     */
    private void adicionarHistorico(StatusOrcamento statusAnterior, StatusOrcamento novoStatus,
            String usuario, String observacao) {
        this.historico.add(HistoricoStatus.criar(statusAnterior, novoStatus, usuario, observacao));
    }

    /**
     * Verifica se o orçamento está pendente.
     *
     * @return true se status é PENDENTE
     */
    public boolean isPendente() {
        return this.status == StatusOrcamento.PENDENTE;
    }

    /**
     * Verifica se o orçamento foi aprovado.
     *
     * @return true se status é APROVADO
     */
    public boolean isAprovado() {
        return this.status == StatusOrcamento.APROVADO;
    }

    /**
     * Verifica se o orçamento foi rejeitado.
     *
     * @return true se status é REJEITADO
     */
    public boolean isRejeitado() {
        return this.status == StatusOrcamento.REJEITADO;
    }
}
