package br.com.grupo99.billingservice.infrastructure.persistence.repository;

import br.com.grupo99.billingservice.infrastructure.persistence.entity.OrcamentoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDbOrcamentoRepository - Testes Unitários")
class DynamoDbOrcamentoRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<OrcamentoEntity> table;

    @Mock
    private PageIterable<OrcamentoEntity> pageIterable;

    private DynamoDbOrcamentoRepository repository;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table);
        repository = new DynamoDbOrcamentoRepository(enhancedClient, "test-");
    }

    @Test
    @DisplayName("Deve usar prefixo da tabela")
    void deveUsarPrefixoTabela() {
        verify(enhancedClient).table(eq("test-orcamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve usar nome padrão quando prefixo é vazio")
    @SuppressWarnings("unchecked")
    void deveUsarNomePadraoQuandoPrefixoVazio() {
        reset(enhancedClient);
        DynamoDbTable<OrcamentoEntity> table2 = mock(DynamoDbTable.class);
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table2);

        new DynamoDbOrcamentoRepository(enhancedClient, "");
        verify(enhancedClient).table(eq("orcamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve usar nome padrão quando prefixo é null")
    @SuppressWarnings("unchecked")
    void deveUsarNomePadraoQuandoPrefixoNull() {
        reset(enhancedClient);
        DynamoDbTable<OrcamentoEntity> table2 = mock(DynamoDbTable.class);
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table2);

        new DynamoDbOrcamentoRepository(enhancedClient, null);
        verify(enhancedClient).table(eq("orcamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve salvar entity gerando ID quando não possui")
    void deveSalvarEntityGerandoId() {
        OrcamentoEntity entity = OrcamentoEntity.builder()
                .osId(UUID.randomUUID().toString())
                .status("PENDENTE")
                .build();

        OrcamentoEntity result = repository.save(entity);

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(table).putItem(entity);
    }

    @Test
    @DisplayName("Deve salvar entity mantendo ID existente")
    void deveSalvarEntityMantendoIdExistente() {
        String existingId = UUID.randomUUID().toString();
        OrcamentoEntity entity = OrcamentoEntity.builder()
                .id(existingId)
                .osId(UUID.randomUUID().toString())
                .status("PENDENTE")
                .build();

        OrcamentoEntity result = repository.save(entity);

        assertEquals(existingId, result.getId());
        verify(table).putItem(entity);
    }

    @Test
    @DisplayName("Deve manter createdAt existente no save")
    void deveManterCreatedAtExistente() {
        Instant createdAt = Instant.now().minusSeconds(3600);
        OrcamentoEntity entity = OrcamentoEntity.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .build();

        OrcamentoEntity result = repository.save(entity);

        assertEquals(createdAt, result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve gerar ID quando ID é blank")
    void deveGerarIdQuandoIdBlank() {
        OrcamentoEntity entity = OrcamentoEntity.builder()
                .id("")
                .build();

        OrcamentoEntity result = repository.save(entity);

        assertNotNull(result.getId());
        assertFalse(result.getId().isBlank());
    }

    @Test
    @DisplayName("Deve buscar entity por ID")
    void deveBuscarEntityPorId() {
        String id = UUID.randomUUID().toString();
        OrcamentoEntity expected = OrcamentoEntity.builder()
                .id(id)
                .status("PENDENTE")
                .build();

        when(table.getItem(any(Key.class))).thenReturn(expected);

        Optional<OrcamentoEntity> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    @DisplayName("Deve retornar empty quando entity não encontrada por ID")
    void deveRetornarEmptyQuandoEntityNaoEncontrada() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        Optional<OrcamentoEntity> result = repository.findById(UUID.randomUUID().toString());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar entity por osId")
    void deveBuscarEntityPorOsId() {
        String osId = UUID.randomUUID().toString();
        OrcamentoEntity expected = OrcamentoEntity.builder()
                .id(UUID.randomUUID().toString())
                .osId(osId)
                .status("PENDENTE")
                .build();

        mockScanWithItems(List.of(expected));

        Optional<OrcamentoEntity> result = repository.findByOsId(osId);

        assertTrue(result.isPresent());
        assertEquals(osId, result.get().getOsId());
    }

    @Test
    @DisplayName("Deve retornar empty quando entity não encontrada por osId")
    void deveRetornarEmptyQuandoNaoEncontradaPorOsId() {
        mockScanWithItems(List.of());

        Optional<OrcamentoEntity> result = repository.findByOsId(UUID.randomUUID().toString());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar entities por status")
    void deveBuscarEntitiesPorStatus() {
        OrcamentoEntity e1 = OrcamentoEntity.builder().id("1").status("PENDENTE").build();
        OrcamentoEntity e2 = OrcamentoEntity.builder().id("2").status("PENDENTE").build();

        mockScanWithItems(List.of(e1, e2));

        List<OrcamentoEntity> result = repository.findByStatus("PENDENTE");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve verificar existência por osId - existe")
    void deveVerificarExistenciaPorOsIdExiste() {
        OrcamentoEntity entity = OrcamentoEntity.builder()
                .id("1")
                .osId("os-1")
                .build();

        mockScanWithItems(List.of(entity));

        assertTrue(repository.existsByOsId("os-1"));
    }

    @Test
    @DisplayName("Deve verificar existência por osId - não existe")
    void deveVerificarExistenciaPorOsIdNaoExiste() {
        mockScanWithItems(List.of());

        assertFalse(repository.existsByOsId("os-inexistente"));
    }

    @Test
    @DisplayName("Deve buscar todas as entities")
    void deveBuscarTodasEntities() {
        OrcamentoEntity e1 = OrcamentoEntity.builder().id("1").build();
        OrcamentoEntity e2 = OrcamentoEntity.builder().id("2").build();
        OrcamentoEntity e3 = OrcamentoEntity.builder().id("3").build();

        // For findAll, scan() is called without parameters
        when(table.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() -> List.of(e1, e2, e3).iterator());

        List<OrcamentoEntity> result = repository.findAll();

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Deve deletar entity por ID")
    void deveDeletarEntityPorId() {
        String id = UUID.randomUUID().toString();

        repository.deleteById(id);

        verify(table).deleteItem(any(Key.class));
    }

    @Test
    @DisplayName("Deve deletar todas as entities")
    void deveDeletarTodasEntities() {
        OrcamentoEntity e1 = OrcamentoEntity.builder().id("1").build();
        OrcamentoEntity e2 = OrcamentoEntity.builder().id("2").build();

        when(table.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() -> List.of(e1, e2).iterator());

        repository.deleteAll();

        verify(table, times(2)).deleteItem(any(Key.class));
    }

    /**
     * Helper: mock scan with ScanEnhancedRequest returning given items
     */
    @SuppressWarnings("unchecked")
    private void mockScanWithItems(List<OrcamentoEntity> items) {
        PageIterable<OrcamentoEntity> scanPageIterable = mock(PageIterable.class);
        when(table.scan(any(ScanEnhancedRequest.class))).thenReturn(scanPageIterable);
        when(scanPageIterable.items()).thenReturn(() -> items.iterator());
    }
}
