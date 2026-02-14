package br.com.grupo99.billingservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root representando um Pagamento.
 * 
 * ✅ CLEAN ARCHITECTURE: Domain sem dependências externas (sem Spring Data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    private UUID id;

    private UUID orcamentoId;

    private UUID osId;

    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;

    private BigDecimal valor;

    private FormaPagamento formaPagamento;

    private String comprovante; // ID/Hash do comprovante de pagamento

    private Instant dataPagamento;

    private Instant dataEstorno;

    private String motivoEstorno;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Factory method para criar novo pagamento.
     *
     * @param orcamentoId    ID do orçamento
     * @param osId           ID da OS
     * @param valor          valor a ser pago
     * @param formaPagamento forma de pagamento
     * @param comprovante    comprovante
     * @return novo Pagamento
     */
    public static Pagamento criar(
            UUID orcamentoId,
            UUID osId,
            BigDecimal valor,
            FormaPagamento formaPagamento,
            String comprovante) {

        return Pagamento.builder()
                .id(UUID.randomUUID())
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status(StatusPagamento.PENDENTE)
                .valor(valor)
                .formaPagamento(formaPagamento)
                .comprovante(comprovante)
                .build();
    }

    /**
     * Factory method para criar novo pagamento (sobrecarga para testes).
     *
     * @param orcamentoId    ID do orçamento
     * @param osId           ID da OS
     * @param valor          valor a ser pago
     * @param formaPagamento forma de pagamento
     * @return novo Pagamento
     */
    public static Pagamento criar(
            UUID orcamentoId,
            UUID osId,
            BigDecimal valor,
            FormaPagamento formaPagamento) {

        if (orcamentoId == null) {
            throw new IllegalArgumentException("ID do orçamento é obrigatório");
        }
        if (osId == null) {
            throw new IllegalArgumentException("ID da OS é obrigatório");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória");
        }

        return Pagamento.builder()
                .id(UUID.randomUUID())
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status(StatusPagamento.PENDENTE)
                .valor(valor)
                .formaPagamento(formaPagamento)
                .build();
    }

    /**
     * Retorna o orcamentoId.
     *
     * @return UUID do orçamento
     */
    public UUID getOrcamentoIdValue() {
        return this.orcamentoId;
    }

    /**
     * Retorna o osId.
     *
     * @return UUID da OS
     */
    public UUID getOsIdValue() {
        return this.osId;
    }

    /**
     * Confirma o pagamento.
     *
     * @throws IllegalStateException se transição não for válida
     */
    public void confirmar() {
        validarTransicao(StatusPagamento.CONFIRMADO);

        this.status = StatusPagamento.CONFIRMADO;
        this.dataPagamento = Instant.now();
    }

    /**
     * Estorna o pagamento.
     *
     * @param motivo motivo do estorno
     * @throws IllegalStateException se transição não for válida
     */
    public void estornar(String motivo) {
        validarTransicao(StatusPagamento.ESTORNADO);

        this.status = StatusPagamento.ESTORNADO;
        this.dataEstorno = Instant.now();
        this.motivoEstorno = motivo;
    }

    /**
     * Estorna o pagamento (sobrecarga para testes sem motivo explícito).
     *
     * @throws IllegalStateException se transição não for válida
     */
    public void estornar() {
        estornar("Estorno solicitado");
    }

    /**
     * Cancela o pagamento.
     *
     * @throws IllegalStateException se transição não for válida
     */
    public void cancelar() {
        validarTransicao(StatusPagamento.CANCELADO);

        this.status = StatusPagamento.CANCELADO;
    }

    /**
     * Valida se a transição de status é permitida.
     *
     * @param novoStatus novo status desejado
     * @throws IllegalStateException se transição não for válida
     */
    private void validarTransicao(StatusPagamento novoStatus) {
        if (!this.status.podeTransicionarPara(novoStatus)) {
            throw new IllegalStateException(
                    String.format("Transição inválida de %s para %s", this.status, novoStatus));
        }
    }

    /**
     * Verifica se o pagamento está confirmado.
     *
     * @return true se status é CONFIRMADO
     */
    public boolean isConfirmado() {
        return this.status == StatusPagamento.CONFIRMADO;
    }

    /**
     * Verifica se o pagamento está pendente.
     *
     * @return true se status é PENDENTE
     */
    public boolean isPendente() {
        return this.status == StatusPagamento.PENDENTE;
    }

    /**
     * Retorna a data de criação (createdAt).
     *
     * @return data de criação
     */
    public Instant getDataCriacao() {
        return this.createdAt;
    }

    /**
     * Retorna a data de confirmação (dataPagamento).
     *
     * @return data de confirmação do pagamento
     */
    public Instant getDataConfirmacao() {
        return this.dataPagamento;
    }
}
