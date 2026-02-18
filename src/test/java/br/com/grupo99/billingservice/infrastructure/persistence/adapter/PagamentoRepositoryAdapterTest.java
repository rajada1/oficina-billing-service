package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.PagamentoEntity;
import br.com.grupo99.billingservice.infrastructure.persistence.repository.DynamoDbPagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoRepositoryAdapter - Testes Unitários")
class PagamentoRepositoryAdapterTest {

    @Mock
    private DynamoDbPagamentoRepository dynamoDbRepository;

    @Mock
    private PagamentoEntityMapper mapper;

    @InjectMocks
    private PagamentoRepositoryAdapter adapter;

    private UUID testId;
    private UUID testOrcamentoId;
    private UUID testOsId;
    private Pagamento testPagamento;
    private PagamentoEntity testEntity;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testOrcamentoId = UUID.randomUUID();
        testOsId = UUID.randomUUID();

        testPagamento = Pagamento.builder()
                .id(testId)
                .orcamentoId(testOrcamentoId)
                .osId(testOsId)
                .status(StatusPagamento.PENDENTE)
                .valor(new BigDecimal("100.00"))
                .build();

        testEntity = PagamentoEntity.builder()
                .id(testId.toString())
                .orcamentoId(testOrcamentoId.toString())
                .osId(testOsId.toString())
                .status("PENDENTE")
                .valor(new BigDecimal("100.00"))
                .build();
    }

    @Test
    @DisplayName("Deve salvar pagamento via adapter")
    void deveSalvarPagamento() {
        when(mapper.toEntity(testPagamento)).thenReturn(testEntity);
        when(dynamoDbRepository.save(testEntity)).thenReturn(testEntity);
        when(mapper.toDomain(testEntity)).thenReturn(testPagamento);

        Pagamento result = adapter.save(testPagamento);

        assertNotNull(result);
        assertEquals(testId, result.getId());
        verify(mapper).toEntity(testPagamento);
        verify(dynamoDbRepository).save(testEntity);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    @DisplayName("Deve buscar pagamento por ID")
    void deveBuscarPorId() {
        when(dynamoDbRepository.findById(testId.toString())).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testPagamento);

        Optional<Pagamento> result = adapter.findById(testId);

        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getId());
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontrado por ID")
    void deveRetornarEmptyQuandoNaoEncontradoPorId() {
        when(dynamoDbRepository.findById(testId.toString())).thenReturn(Optional.empty());

        Optional<Pagamento> result = adapter.findById(testId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar pagamento por orcamentoId")
    void deveBuscarPorOrcamentoId() {
        when(dynamoDbRepository.findByOrcamentoId(testOrcamentoId.toString())).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testPagamento);

        Optional<Pagamento> result = adapter.findByOrcamentoId(testOrcamentoId);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontrado por orcamentoId")
    void deveRetornarEmptyQuandoNaoEncontradoPorOrcamentoId() {
        when(dynamoDbRepository.findByOrcamentoId(testOrcamentoId.toString())).thenReturn(Optional.empty());

        Optional<Pagamento> result = adapter.findByOrcamentoId(testOrcamentoId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar pagamentos por osId")
    void deveBuscarPorOsId() {
        PagamentoEntity e2 = PagamentoEntity.builder().id(UUID.randomUUID().toString()).build();
        Pagamento p2 = Pagamento.builder().id(UUID.randomUUID()).build();

        when(dynamoDbRepository.findByOsId(testOsId.toString())).thenReturn(List.of(testEntity, e2));
        when(mapper.toDomain(testEntity)).thenReturn(testPagamento);
        when(mapper.toDomain(e2)).thenReturn(p2);

        List<Pagamento> result = adapter.findByOsId(testOsId);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve buscar pagamentos por status")
    void deveBuscarPorStatus() {
        when(dynamoDbRepository.findByStatus("CONFIRMADO")).thenReturn(List.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testPagamento);

        List<Pagamento> result = adapter.findByStatus(StatusPagamento.CONFIRMADO);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve verificar existência por orcamentoId e status")
    void deveVerificarExistenciaPorOrcamentoIdEStatus() {
        when(dynamoDbRepository.existsByOrcamentoIdAndStatus(testOrcamentoId.toString(), "CONFIRMADO"))
                .thenReturn(true);

        assertTrue(adapter.existsByOrcamentoIdAndStatus(testOrcamentoId, StatusPagamento.CONFIRMADO));
    }

    @Test
    @DisplayName("Deve retornar false quando não existe por orcamentoId e status")
    void deveRetornarFalseQuandoNaoExiste() {
        when(dynamoDbRepository.existsByOrcamentoIdAndStatus(testOrcamentoId.toString(), "CONFIRMADO"))
                .thenReturn(false);

        assertFalse(adapter.existsByOrcamentoIdAndStatus(testOrcamentoId, StatusPagamento.CONFIRMADO));
    }

    @Test
    @DisplayName("Deve deletar pagamento por ID")
    void deveDeletarPorId() {
        adapter.deleteById(testId);

        verify(dynamoDbRepository).deleteById(testId.toString());
    }
}
