package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.*;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.OrcamentoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrcamentoEntityMapper - Testes Unitários")
class OrcamentoEntityMapperTest {

    private OrcamentoEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrcamentoEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("Deve retornar null quando orcamento é null")
        void deveRetornarNullQuandoOrcamentoNull() {
            assertNull(mapper.toEntity(null));
        }

        @Test
        @DisplayName("Deve converter orcamento completo para entity")
        void deveConverterOrcamentoCompletoParaEntity() {
            UUID id = UUID.randomUUID();
            UUID osId = UUID.randomUUID();
            Instant now = Instant.now();

            List<ItemOrcamento> itens = List.of(
                    ItemOrcamento.builder()
                            .descricao("Troca de óleo")
                            .valorUnitario(new BigDecimal("50.00"))
                            .valorTotal(new BigDecimal("50.00"))
                            .quantidade(1)
                            .tipo(TipoItem.SERVICO)
                            .build(),
                    ItemOrcamento.builder()
                            .descricao("Filtro de óleo")
                            .valorUnitario(new BigDecimal("30.00"))
                            .valorTotal(new BigDecimal("60.00"))
                            .quantidade(2)
                            .tipo(TipoItem.PECA)
                            .build());

            List<HistoricoStatus> historico = List.of(
                    HistoricoStatus.builder()
                            .statusAnterior(null)
                            .novoStatus(StatusOrcamento.PENDENTE)
                            .usuario("system")
                            .observacao("Criado")
                            .data(now)
                            .build(),
                    HistoricoStatus.builder()
                            .statusAnterior(StatusOrcamento.PENDENTE)
                            .novoStatus(StatusOrcamento.APROVADO)
                            .usuario("admin")
                            .observacao("Aprovado pelo cliente")
                            .data(now)
                            .build());

            Orcamento orcamento = Orcamento.builder()
                    .id(id)
                    .osId(osId)
                    .status(StatusOrcamento.APROVADO)
                    .itens(itens)
                    .valorTotal(new BigDecimal("110.00"))
                    .dataGeracao(now)
                    .dataAprovacao(now)
                    .dataRejeicao(null)
                    .observacao("Teste")
                    .motivoRejeicao(null)
                    .historico(historico)
                    .build();

            OrcamentoEntity entity = mapper.toEntity(orcamento);

            assertNotNull(entity);
            assertEquals(id.toString(), entity.getId());
            assertEquals(osId.toString(), entity.getOsId());
            assertEquals("APROVADO", entity.getStatus());
            assertEquals(new BigDecimal("110.00"), entity.getValorTotal());
            assertEquals(now, entity.getDataGeracao());
            assertEquals(now, entity.getDataAprovacao());
            assertNull(entity.getDataRejeicao());
            assertEquals("Teste", entity.getObservacao());
            assertNull(entity.getMotivoRejeicao());

            // Itens
            assertEquals(2, entity.getItens().size());
            assertEquals("Troca de óleo", entity.getItens().get(0).getDescricao());
            assertEquals("SERVICO", entity.getItens().get(0).getTipo());
            assertEquals("Filtro de óleo", entity.getItens().get(1).getDescricao());
            assertEquals("PECA", entity.getItens().get(1).getTipo());

            // Histórico
            assertEquals(2, entity.getHistorico().size());
            assertNull(entity.getHistorico().get(0).getStatusAnterior());
            assertEquals("PENDENTE", entity.getHistorico().get(0).getNovoStatus());
            assertEquals("PENDENTE", entity.getHistorico().get(1).getStatusAnterior());
            assertEquals("APROVADO", entity.getHistorico().get(1).getNovoStatus());
        }

        @Test
        @DisplayName("Deve converter orcamento com listas nulas")
        void deveConverterOrcamentoComListasNulas() {
            Orcamento orcamento = Orcamento.builder()
                    .id(UUID.randomUUID())
                    .osId(UUID.randomUUID())
                    .status(StatusOrcamento.PENDENTE)
                    .itens(null)
                    .historico(null)
                    .build();

            OrcamentoEntity entity = mapper.toEntity(orcamento);

            assertNotNull(entity);
            assertTrue(entity.getItens().isEmpty());
            assertTrue(entity.getHistorico().isEmpty());
        }

        @Test
        @DisplayName("Deve converter orcamento com id e osId nulos")
        void deveConverterOrcamentoComIdsNulos() {
            Orcamento orcamento = Orcamento.builder()
                    .id(null)
                    .osId(null)
                    .status(null)
                    .build();

            OrcamentoEntity entity = mapper.toEntity(orcamento);

            assertNotNull(entity);
            assertNull(entity.getId());
            assertNull(entity.getOsId());
            assertEquals("PENDENTE", entity.getStatus()); // default when status is null
        }

        @Test
        @DisplayName("Deve converter item com tipo nulo")
        void deveConverterItemComTipoNulo() {
            Orcamento orcamento = Orcamento.builder()
                    .id(UUID.randomUUID())
                    .status(StatusOrcamento.PENDENTE)
                    .itens(List.of(ItemOrcamento.builder()
                            .descricao("Sem tipo")
                            .tipo(null)
                            .build()))
                    .historico(new ArrayList<>())
                    .build();

            OrcamentoEntity entity = mapper.toEntity(orcamento);

            assertNull(entity.getItens().get(0).getTipo());
        }

        @Test
        @DisplayName("Deve converter historico com status anterior nulo")
        void deveConverterHistoricoComStatusAnteriorNulo() {
            Orcamento orcamento = Orcamento.builder()
                    .id(UUID.randomUUID())
                    .status(StatusOrcamento.PENDENTE)
                    .itens(new ArrayList<>())
                    .historico(List.of(HistoricoStatus.builder()
                            .statusAnterior(null)
                            .novoStatus(null)
                            .build()))
                    .build();

            OrcamentoEntity entity = mapper.toEntity(orcamento);

            assertNull(entity.getHistorico().get(0).getStatusAnterior());
            assertNull(entity.getHistorico().get(0).getNovoStatus());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("Deve retornar null quando entity é null")
        void deveRetornarNullQuandoEntityNull() {
            assertNull(mapper.toDomain(null));
        }

        @Test
        @DisplayName("Deve converter entity completa para domain")
        void deveConverterEntityCompletaParaDomain() {
            UUID id = UUID.randomUUID();
            UUID osId = UUID.randomUUID();
            Instant now = Instant.now();

            List<OrcamentoEntity.ItemOrcamentoEntity> itensEntity = List.of(
                    OrcamentoEntity.ItemOrcamentoEntity.builder()
                            .descricao("Serviço A")
                            .valorUnitario(new BigDecimal("100.00"))
                            .valorTotal(new BigDecimal("200.00"))
                            .quantidade(2)
                            .tipo("SERVICO")
                            .build(),
                    OrcamentoEntity.ItemOrcamentoEntity.builder()
                            .descricao("Peça B")
                            .valorUnitario(new BigDecimal("75.00"))
                            .valorTotal(new BigDecimal("75.00"))
                            .quantidade(1)
                            .tipo("PECA")
                            .build());

            List<OrcamentoEntity.HistoricoStatusEntity> historicoEntity = List.of(
                    OrcamentoEntity.HistoricoStatusEntity.builder()
                            .statusAnterior(null)
                            .novoStatus("PENDENTE")
                            .usuario("system")
                            .observacao("Criado")
                            .data(now)
                            .build(),
                    OrcamentoEntity.HistoricoStatusEntity.builder()
                            .statusAnterior("PENDENTE")
                            .novoStatus("REJEITADO")
                            .usuario("cliente")
                            .observacao("Muito caro")
                            .data(now)
                            .build());

            OrcamentoEntity entity = OrcamentoEntity.builder()
                    .id(id.toString())
                    .osId(osId.toString())
                    .status("REJEITADO")
                    .itens(itensEntity)
                    .valorTotal(new BigDecimal("275.00"))
                    .dataGeracao(now)
                    .dataAprovacao(null)
                    .dataRejeicao(now)
                    .observacao("Obs teste")
                    .motivoRejeicao("Muito caro")
                    .historico(historicoEntity)
                    .build();

            Orcamento domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(id, domain.getId());
            assertEquals(osId, domain.getOsId());
            assertEquals(StatusOrcamento.REJEITADO, domain.getStatus());
            assertEquals(new BigDecimal("275.00"), domain.getValorTotal());
            assertEquals(now, domain.getDataGeracao());
            assertNull(domain.getDataAprovacao());
            assertEquals(now, domain.getDataRejeicao());
            assertEquals("Obs teste", domain.getObservacao());
            assertEquals("Muito caro", domain.getMotivoRejeicao());

            // Itens
            assertEquals(2, domain.getItens().size());
            assertEquals("Serviço A", domain.getItens().get(0).getDescricao());
            assertEquals(TipoItem.SERVICO, domain.getItens().get(0).getTipo());
            assertEquals("Peça B", domain.getItens().get(1).getDescricao());
            assertEquals(TipoItem.PECA, domain.getItens().get(1).getTipo());

            // Histórico
            assertEquals(2, domain.getHistorico().size());
            assertNull(domain.getHistorico().get(0).getStatusAnterior());
            assertEquals(StatusOrcamento.PENDENTE, domain.getHistorico().get(0).getNovoStatus());
            assertEquals(StatusOrcamento.PENDENTE, domain.getHistorico().get(1).getStatusAnterior());
            assertEquals(StatusOrcamento.REJEITADO, domain.getHistorico().get(1).getNovoStatus());
        }

        @Test
        @DisplayName("Deve converter entity com listas nulas")
        void deveConverterEntityComListasNulas() {
            OrcamentoEntity entity = OrcamentoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .osId(UUID.randomUUID().toString())
                    .status("PENDENTE")
                    .itens(null)
                    .historico(null)
                    .build();

            Orcamento domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertTrue(domain.getItens().isEmpty());
            assertTrue(domain.getHistorico().isEmpty());
        }

        @Test
        @DisplayName("Deve converter entity com ids nulos")
        void deveConverterEntityComIdsNulos() {
            OrcamentoEntity entity = OrcamentoEntity.builder()
                    .id(null)
                    .osId(null)
                    .status(null)
                    .itens(new ArrayList<>())
                    .historico(new ArrayList<>())
                    .build();

            Orcamento domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertNull(domain.getId());
            assertNull(domain.getOsId());
            assertEquals(StatusOrcamento.PENDENTE, domain.getStatus()); // default
        }

        @Test
        @DisplayName("Deve converter item entity com tipo nulo")
        void deveConverterItemEntityComTipoNulo() {
            OrcamentoEntity entity = OrcamentoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .status("PENDENTE")
                    .itens(List.of(OrcamentoEntity.ItemOrcamentoEntity.builder()
                            .descricao("Sem tipo")
                            .tipo(null)
                            .build()))
                    .historico(new ArrayList<>())
                    .build();

            Orcamento domain = mapper.toDomain(entity);

            assertNull(domain.getItens().get(0).getTipo());
        }

        @Test
        @DisplayName("Deve converter historico entity com status nulos")
        void deveConverterHistoricoEntityComStatusNulos() {
            OrcamentoEntity entity = OrcamentoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .status("PENDENTE")
                    .itens(new ArrayList<>())
                    .historico(List.of(OrcamentoEntity.HistoricoStatusEntity.builder()
                            .statusAnterior(null)
                            .novoStatus(null)
                            .usuario("test")
                            .build()))
                    .build();

            Orcamento domain = mapper.toDomain(entity);

            assertNull(domain.getHistorico().get(0).getStatusAnterior());
            assertNull(domain.getHistorico().get(0).getNovoStatus());
        }
    }
}
