package br.com.grupo99.billingservice.infrastructure.repository;

import br.com.grupo99.billingservice.domain.model.*;
import br.com.grupo99.billingservice.infrastructure.persistence.adapter.OrcamentoEntityMapper;
import br.com.grupo99.billingservice.infrastructure.persistence.adapter.OrcamentoRepositoryAdapter;
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

/**
 * Testes unitários para OrcamentoRepositoryAdapter.
 * Utiliza mocks para DynamoDbOrcamentoRepository e OrcamentoEntityMapper
 * para testar a lógica do adapter sem necessidade de Docker/Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrcamentoRepository - Testes Unitários (com Mocks)")
class OrcamentoRepositoryTest {

    @Mock
    private DynamoDbOrcamentoRepository dynamoDbRepository;

    @Mock
    private OrcamentoEntityMapper mapper;

    @InjectMocks
    private OrcamentoRepositoryAdapter repository;

    private UUID osId;
    private Orcamento orcamento;
    private OrcamentoEntity orcamentoEntity;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        orcamento = Orcamento.criar(osId, "Orçamento de teste");
        orcamentoEntity = new OrcamentoEntity();
        orcamentoEntity.setId(orcamento.getId().toString());
        orcamentoEntity.setOsId(osId.toString());
        orcamentoEntity.setStatus("PENDENTE");
    }

    @Test
    @DisplayName("Deve salvar orçamento com sucesso")
    void deveSalvarOrcamentoComSucesso() {
        // Arrange
        when(mapper.toEntity(any(Orcamento.class))).thenReturn(orcamentoEntity);
        when(dynamoDbRepository.save(any(OrcamentoEntity.class))).thenReturn(orcamentoEntity);
        when(mapper.toDomain(any(OrcamentoEntity.class))).thenReturn(orcamento);

        // Act
        Orcamento saved = repository.save(orcamento);

        // Assert
        assertNotNull(saved);
        assertEquals(osId, saved.getOsId());
        verify(dynamoDbRepository).save(any(OrcamentoEntity.class));
        verify(mapper).toEntity(orcamento);
        verify(mapper).toDomain(orcamentoEntity);
    }

    @Test
    @DisplayName("Deve buscar orçamento por osId")
    void deveBuscarOrcamentoPorOsId() {
        // Arrange
        when(dynamoDbRepository.findByOsId(osId.toString())).thenReturn(Optional.of(orcamentoEntity));
        when(mapper.toDomain(orcamentoEntity)).thenReturn(orcamento);

        // Act
        Optional<Orcamento> found = repository.findByOsId(osId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(osId, found.get().getOsId());
        verify(dynamoDbRepository).findByOsId(osId.toString());
    }

    @Test
    @DisplayName("Deve buscar orçamentos por status")
    void deveBuscarOrcamentosPorStatus() {
        // Arrange
        Orcamento orc1 = Orcamento.criar(UUID.randomUUID(), "Orc 1");

        when(dynamoDbRepository.findByStatus("PENDENTE")).thenReturn(List.of(orcamentoEntity));
        when(mapper.toDomain(orcamentoEntity)).thenReturn(orc1);

        // Act
        List<Orcamento> pendentes = repository.findByStatus(StatusOrcamento.PENDENTE);

        // Assert
        assertEquals(1, pendentes.size());
        verify(dynamoDbRepository).findByStatus("PENDENTE");
    }

    @Test
    @DisplayName("Deve verificar existência por osId")
    void deveVerificarExistenciaPorOsId() {
        // Arrange
        UUID existingOsId = UUID.randomUUID();
        UUID nonExistingOsId = UUID.randomUUID();

        when(dynamoDbRepository.existsByOsId(existingOsId.toString())).thenReturn(true);
        when(dynamoDbRepository.existsByOsId(nonExistingOsId.toString())).thenReturn(false);

        // Act
        boolean existe = repository.existsByOsId(existingOsId);
        boolean naoExiste = repository.existsByOsId(nonExistingOsId);

        // Assert
        assertTrue(existe);
        assertFalse(naoExiste);
    }

    @Test
    @DisplayName("Deve buscar orçamento por id")
    void deveBuscarOrcamentoPorId() {
        // Arrange
        UUID id = orcamento.getId();
        when(dynamoDbRepository.findById(id.toString())).thenReturn(Optional.of(orcamentoEntity));
        when(mapper.toDomain(orcamentoEntity)).thenReturn(orcamento);

        // Act
        Optional<Orcamento> found = repository.findById(id);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        verify(dynamoDbRepository).findById(id.toString());
    }
}
