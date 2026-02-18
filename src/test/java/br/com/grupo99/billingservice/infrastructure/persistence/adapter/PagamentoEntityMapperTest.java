package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.FormaPagamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.PagamentoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PagamentoEntityMapper - Testes Unitários")
class PagamentoEntityMapperTest {

    private PagamentoEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PagamentoEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("Deve retornar null quando pagamento é null")
        void deveRetornarNullQuandoPagamentoNull() {
            assertNull(mapper.toEntity(null));
        }

        @Test
        @DisplayName("Deve converter pagamento completo para entity")
        void deveConverterPagamentoCompletoParaEntity() {
            UUID id = UUID.randomUUID();
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();
            Instant now = Instant.now();

            Pagamento pagamento = Pagamento.builder()
                    .id(id)
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .status(StatusPagamento.CONFIRMADO)
                    .valor(new BigDecimal("500.00"))
                    .formaPagamento(FormaPagamento.PIX)
                    .comprovante("comp-123")
                    .dataPagamento(now)
                    .dataEstorno(null)
                    .motivoEstorno(null)
                    .build();

            PagamentoEntity entity = mapper.toEntity(pagamento);

            assertNotNull(entity);
            assertEquals(id.toString(), entity.getId());
            assertEquals(orcamentoId.toString(), entity.getOrcamentoId());
            assertEquals(osId.toString(), entity.getOsId());
            assertEquals("CONFIRMADO", entity.getStatus());
            assertEquals(new BigDecimal("500.00"), entity.getValor());
            assertEquals("PIX", entity.getFormaPagamento());
            assertEquals("comp-123", entity.getComprovante());
            assertEquals(now, entity.getDataPagamento());
            assertNull(entity.getDataEstorno());
            assertNull(entity.getMotivoEstorno());
        }

        @Test
        @DisplayName("Deve converter pagamento com ids nulos")
        void deveConverterPagamentoComIdsNulos() {
            Pagamento pagamento = Pagamento.builder()
                    .id(null)
                    .orcamentoId(null)
                    .osId(null)
                    .status(null)
                    .formaPagamento(null)
                    .build();

            PagamentoEntity entity = mapper.toEntity(pagamento);

            assertNotNull(entity);
            assertNull(entity.getId());
            assertNull(entity.getOrcamentoId());
            assertNull(entity.getOsId());
            assertEquals("PENDENTE", entity.getStatus());
            assertNull(entity.getFormaPagamento());
        }

        @Test
        @DisplayName("Deve converter pagamento estornado")
        void deveConverterPagamentoEstornado() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            Pagamento pagamento = Pagamento.builder()
                    .id(id)
                    .orcamentoId(UUID.randomUUID())
                    .osId(UUID.randomUUID())
                    .status(StatusPagamento.ESTORNADO)
                    .valor(new BigDecimal("200.00"))
                    .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                    .dataEstorno(now)
                    .motivoEstorno("Serviço não concluído")
                    .build();

            PagamentoEntity entity = mapper.toEntity(pagamento);

            assertEquals("ESTORNADO", entity.getStatus());
            assertEquals("CARTAO_CREDITO", entity.getFormaPagamento());
            assertEquals(now, entity.getDataEstorno());
            assertEquals("Serviço não concluído", entity.getMotivoEstorno());
        }

        @Test
        @DisplayName("Deve converter todas as formas de pagamento")
        void deveConverterTodasFormasDePagamento() {
            for (FormaPagamento fp : FormaPagamento.values()) {
                Pagamento pagamento = Pagamento.builder()
                        .id(UUID.randomUUID())
                        .status(StatusPagamento.PENDENTE)
                        .formaPagamento(fp)
                        .build();

                PagamentoEntity entity = mapper.toEntity(pagamento);
                assertEquals(fp.name(), entity.getFormaPagamento());
            }
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
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();
            Instant now = Instant.now();

            PagamentoEntity entity = PagamentoEntity.builder()
                    .id(id.toString())
                    .orcamentoId(orcamentoId.toString())
                    .osId(osId.toString())
                    .status("CONFIRMADO")
                    .valor(new BigDecimal("350.00"))
                    .formaPagamento("BOLETO")
                    .comprovante("boleto-456")
                    .dataPagamento(now)
                    .dataEstorno(null)
                    .motivoEstorno(null)
                    .build();

            Pagamento domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(id, domain.getId());
            assertEquals(orcamentoId, domain.getOrcamentoId());
            assertEquals(osId, domain.getOsId());
            assertEquals(StatusPagamento.CONFIRMADO, domain.getStatus());
            assertEquals(new BigDecimal("350.00"), domain.getValor());
            assertEquals(FormaPagamento.BOLETO, domain.getFormaPagamento());
            assertEquals("boleto-456", domain.getComprovante());
            assertEquals(now, domain.getDataPagamento());
            assertNull(domain.getDataEstorno());
            assertNull(domain.getMotivoEstorno());
        }

        @Test
        @DisplayName("Deve converter entity com ids nulos")
        void deveConverterEntityComIdsNulos() {
            PagamentoEntity entity = PagamentoEntity.builder()
                    .id(null)
                    .orcamentoId(null)
                    .osId(null)
                    .status(null)
                    .formaPagamento(null)
                    .build();

            Pagamento domain = mapper.toDomain(entity);

            assertNotNull(domain);
            assertNull(domain.getId());
            assertNull(domain.getOrcamentoId());
            assertNull(domain.getOsId());
            assertEquals(StatusPagamento.PENDENTE, domain.getStatus());
            assertNull(domain.getFormaPagamento());
        }

        @Test
        @DisplayName("Deve converter entity estornada")
        void deveConverterEntityEstornada() {
            Instant now = Instant.now();

            PagamentoEntity entity = PagamentoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .orcamentoId(UUID.randomUUID().toString())
                    .osId(UUID.randomUUID().toString())
                    .status("ESTORNADO")
                    .valor(new BigDecimal("150.00"))
                    .formaPagamento("DINHEIRO")
                    .dataEstorno(now)
                    .motivoEstorno("Defeito encontrado")
                    .build();

            Pagamento domain = mapper.toDomain(entity);

            assertEquals(StatusPagamento.ESTORNADO, domain.getStatus());
            assertEquals(FormaPagamento.DINHEIRO, domain.getFormaPagamento());
            assertEquals(now, domain.getDataEstorno());
            assertEquals("Defeito encontrado", domain.getMotivoEstorno());
        }

        @Test
        @DisplayName("Deve converter entity cancelada")
        void deveConverterEntityCancelada() {
            PagamentoEntity entity = PagamentoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .orcamentoId(UUID.randomUUID().toString())
                    .status("CANCELADO")
                    .formaPagamento("TRANSFERENCIA")
                    .build();

            Pagamento domain = mapper.toDomain(entity);

            assertEquals(StatusPagamento.CANCELADO, domain.getStatus());
            assertEquals(FormaPagamento.TRANSFERENCIA, domain.getFormaPagamento());
        }
    }
}
