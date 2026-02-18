package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;
import br.com.grupo99.billingservice.infrastructure.persistence.entity.OrcamentoEntity;
import br.com.grupo99.billingservice.infrastructure.persistence.repository.DynamoDbOrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrcamentoRepositoryAdapter - Testes Unitários")
class OrcamentoRepositoryAdapterTest {

    @Mock
    private DynamoDbOrcamentoRepository dynamoDbRepository;

    @Mock
    private OrcamentoEntityMapper mapper;

    @InjectMocks
    private OrcamentoRepositoryAdapter adapter;

    private UUID testId;
    private UUID testOsId;
    private Orcamento testOrcamento;
    private OrcamentoEntity testEntity;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testOsId = UUID.randomUUID();

        testOrcamento = Orcamento.builder()
                .id(testId)
                .osId(testOsId)
                .status(StatusOrcamento.PENDENTE)
                .build();

        testEntity = OrcamentoEntity.builder()
                .id(testId.toString())
                .osId(testOsId.toString())
                .status("PENDENTE")
                .build();
    }

    @Test
    @DisplayName("Deve salvar orcamento via adapter")
    void deveSalvarOrcamento() {
        when(mapper.toEntity(testOrcamento)).thenReturn(testEntity);
        when(dynamoDbRepository.save(testEntity)).thenReturn(testEntity);
        when(mapper.toDomain(testEntity)).thenReturn(testOrcamento);

        Orcamento result = adapter.save(testOrcamento);

        assertNotNull(result);
        assertEquals(testId, result.getId());
        verify(mapper).toEntity(testOrcamento);
        verify(dynamoDbRepository).save(testEntity);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    @DisplayName("Deve buscar orcamento por ID")
    void deveBuscarPorId() {
        when(dynamoDbRepository.findById(testId.toString())).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testOrcamento);

        Optional<Orcamento> result = adapter.findById(testId);

        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getId());
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontrado por ID")
    void deveRetornarEmptyQuandoNaoEncontradoPorId() {
        when(dynamoDbRepository.findById(testId.toString())).thenReturn(Optional.empty());

        Optional<Orcamento> result = adapter.findById(testId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar orcamento por osId")
    void deveBuscarPorOsId() {
        when(dynamoDbRepository.findByOsId(testOsId.toString())).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testOrcamento);

        Optional<Orcamento> result = adapter.findByOsId(testOsId);

        assertTrue(result.isPresent());
        assertEquals(testOsId, result.get().getOsId());
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontrado por osId")
    void deveRetornarEmptyQuandoNaoEncontradoPorOsId() {
        when(dynamoDbRepository.findByOsId(testOsId.toString())).thenReturn(Optional.empty());

        Optional<Orcamento> result = adapter.findByOsId(testOsId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar orcamentos por status")
    void deveBuscarPorStatus() {
        OrcamentoEntity e2 = OrcamentoEntity.builder().id(UUID.randomUUID().toString()).status("PENDENTE").build();
        Orcamento o2 = Orcamento.builder().id(UUID.randomUUID()).status(StatusOrcamento.PENDENTE).build();

        when(dynamoDbRepository.findByStatus("PENDENTE")).thenReturn(List.of(testEntity, e2));
        when(mapper.toDomain(testEntity)).thenReturn(testOrcamento);
        when(mapper.toDomain(e2)).thenReturn(o2);

        List<Orcamento> result = adapter.findByStatus(StatusOrcamento.PENDENTE);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve verificar existência por osId")
    void deveVerificarExistenciaPorOsId() {
        when(dynamoDbRepository.existsByOsId(testOsId.toString())).thenReturn(true);

        assertTrue(adapter.existsByOsId(testOsId));
    }

    @Test
    @DisplayName("Deve retornar false quando não existe por osId")
    void deveRetornarFalseQuandoNaoExistePorOsId() {
        when(dynamoDbRepository.existsByOsId(testOsId.toString())).thenReturn(false);

        assertFalse(adapter.existsByOsId(testOsId));
    }

    @Test
    @DisplayName("Deve deletar orcamento por ID")
    void deveDeletarPorId() {
        adapter.deleteById(testId);

        verify(dynamoDbRepository).deleteById(testId.toString());
    }
}
