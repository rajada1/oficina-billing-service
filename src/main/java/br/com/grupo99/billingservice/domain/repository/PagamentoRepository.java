package br.com.grupo99.billingservice.domain.repository;

import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para agregado Pagamento.
 * 
 * ✅ CLEAN ARCHITECTURE: Interface pura de domínio (sem Spring Data, sem
 * anotações)
 * Implementação fica na camada infrastructure
 */
public interface PagamentoRepository {

    /**
     * Salva um pagamento.
     *
     * @param pagamento pagamento a ser salvo
     * @return pagamento salvo
     */
    Pagamento save(Pagamento pagamento);

    /**
     * Busca pagamento por ID.
     *
     * @param id ID do pagamento
     * @return Optional contendo o pagamento, se encontrado
     */
    Optional<Pagamento> findById(UUID id);

    /**
     * Busca pagamento por ID do orçamento.
     *
     * @param orcamentoId ID do orçamento
     * @return Optional contendo o pagamento, se encontrado
     */
    Optional<Pagamento> findByOrcamentoId(UUID orcamentoId);

    /**
     * Busca pagamentos por ID da OS.
     *
     * @param osId ID da ordem de serviço
     * @return lista de pagamentos
     */
    List<Pagamento> findByOsId(UUID osId);

    /**
     * Busca pagamentos por status.
     *
     * @param status status do pagamento
     * @return lista de pagamentos
     */
    List<Pagamento> findByStatus(StatusPagamento status);

    /**
     * Verifica se existe pagamento confirmado para um orçamento.
     *
     * @param orcamentoId ID do orçamento
     * @param status      status do pagamento
     * @return true se existe
     */
    boolean existsByOrcamentoIdAndStatus(UUID orcamentoId, StatusPagamento status);

    /**
     * Deleta um pagamento.
     *
     * @param id ID do pagamento a ser deletado
     */
    void deleteById(UUID id);
}
