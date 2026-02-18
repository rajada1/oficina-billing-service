package br.com.grupo99.billingservice.domain.repository;

import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para agregado Orcamento.
 * 
 * ✅ CLEAN ARCHITECTURE: Interface pura de domínio (sem Spring Data, sem
 * anotações)
 * Implementação fica na camada infrastructure
 */
public interface OrcamentoRepository {

    /**
     * Salva um orçamento.
     *
     * @param orcamento orçamento a ser salvo
     * @return orçamento salvo
     */
    Orcamento save(Orcamento orcamento);

    /**
     * Busca orçamento por ID.
     *
     * @param id ID do orçamento
     * @return Optional contendo o orçamento, se encontrado
     */
    Optional<Orcamento> findById(UUID id);

    /**
     * Busca orçamento por ID da OS.
     *
     * @param osId ID da ordem de serviço
     * @return Optional contendo o orçamento, se encontrado
     */
    Optional<Orcamento> findByOsId(UUID osId);

    /**
     * Busca orçamentos por status.
     *
     * @param status status do orçamento
     * @return lista de orçamentos
     */
    List<Orcamento> findByStatus(StatusOrcamento status);

    /**
     * Verifica se existe orçamento para uma OS.
     *
     * @param osId ID da ordem de serviço
     * @return true se existe
     */
    boolean existsByOsId(UUID osId);

    /**
     * Busca todos os orçamentos.
     *
     * @return lista de orçamentos
     */
    List<Orcamento> findAll();

    /**
     * Deleta um orçamento.
     *
     * @param id ID do orçamento a ser deletado
     */
    void deleteById(UUID id);
}
