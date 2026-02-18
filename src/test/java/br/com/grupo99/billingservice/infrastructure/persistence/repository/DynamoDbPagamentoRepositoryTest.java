package br.com.grupo99.billingservice.infrastructure.persistence.repository;

import br.com.grupo99.billingservice.infrastructure.persistence.entity.PagamentoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDbPagamentoRepository - Testes Unitários")
class DynamoDbPagamentoRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<PagamentoEntity> table;

    private DynamoDbPagamentoRepository repository;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table);
        repository = new DynamoDbPagamentoRepository(enhancedClient, "test-");
    }

    @Test
    @DisplayName("Deve usar prefixo da tabela")
    void deveUsarPrefixoTabela() {
        verify(enhancedClient).table(eq("test-pagamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve usar nome padrão quando prefixo é vazio")
    @SuppressWarnings("unchecked")
    void deveUsarNomePadraoQuandoPrefixoVazio() {
        reset(enhancedClient);
        DynamoDbTable<PagamentoEntity> table2 = mock(DynamoDbTable.class);
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table2);

        new DynamoDbPagamentoRepository(enhancedClient, "");
        verify(enhancedClient).table(eq("pagamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve usar nome padrão quando prefixo é null")
    @SuppressWarnings("unchecked")
    void deveUsarNomePadraoQuandoPrefixoNull() {
        reset(enhancedClient);
        DynamoDbTable<PagamentoEntity> table2 = mock(DynamoDbTable.class);
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table2);

        new DynamoDbPagamentoRepository(enhancedClient, null);
        verify(enhancedClient).table(eq("pagamentos"), any(TableSchema.class));
    }

    @Test
    @DisplayName("Deve salvar entity gerando ID quando não possui")
    void deveSalvarEntityGerandoId() {
        PagamentoEntity entity = PagamentoEntity.builder()
                .orcamentoId(UUID.randomUUID().toString())
                .status("PENDENTE")
                .build();

        PagamentoEntity result = repository.save(entity);

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(table).putItem(entity);
    }

    @Test
    @DisplayName("Deve salvar entity mantendo ID existente")
    void deveSalvarEntityMantendoIdExistente() {
        String existingId = UUID.randomUUID().toString();
        PagamentoEntity entity = PagamentoEntity.builder()
                .id(existingId)
                .status("PENDENTE")
                .build();

        PagamentoEntity result = repository.save(entity);

        assertEquals(existingId, result.getId());
        verify(table).putItem(entity);
    }

    @Test
    @DisplayName("Deve manter createdAt existente no save")
    void deveManterCreatedAtExistente() {
        Instant createdAt = Instant.now().minusSeconds(3600);
        PagamentoEntity entity = PagamentoEntity.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .build();

        PagamentoEntity result = repository.save(entity);

        assertEquals(createdAt, result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve gerar ID quando ID é blank")
    void deveGerarIdQuandoIdBlank() {
        PagamentoEntity entity = PagamentoEntity.builder()
                .id("")
                .build();

        PagamentoEntity result = repository.save(entity);

        assertNotNull(result.getId());
        assertFalse(result.getId().isBlank());
    }

    @Test
    @DisplayName("Deve buscar entity por ID")
    void deveBuscarEntityPorId() {
        String id = UUID.randomUUID().toString();
        PagamentoEntity expected = PagamentoEntity.builder()
                .id(id)
                .status("PENDENTE")
                .build();

        when(table.getItem(any(Key.class))).thenReturn(expected);

        Optional<PagamentoEntity> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    @DisplayName("Deve retornar empty quando entity não encontrada por ID")
    void deveRetornarEmptyQuandoNaoEncontrada() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        Optional<PagamentoEntity> result = repository.findById(UUID.randomUUID().toString());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar entity por orcamentoId")
    void deveBuscarEntityPorOrcamentoId() {
        String orcamentoId = UUID.randomUUID().toString();
        PagamentoEntity expected = PagamentoEntity.builder()
                .id(UUID.randomUUID().toString())
                .orcamentoId(orcamentoId)
                .build();

        mockScanWithItems(List.of(expected));

        Optional<PagamentoEntity> result = repository.findByOrcamentoId(orcamentoId);

        assertTrue(result.isPresent());
        assertEquals(orcamentoId, result.get().getOrcamentoId());
    }

    @Test
    @DisplayName("Deve retornar empty quando não encontrada por orcamentoId")
    void deveRetornarEmptyQuandoNaoEncontradaPorOrcamentoId() {
        mockScanWithItems(List.of());

        Optional<PagamentoEntity> result = repository.findByOrcamentoId(UUID.randomUUID().toString());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve buscar entities por osId")
    void deveBuscarEntitiesPorOsId() {
        String osId = UUID.randomUUID().toString();
        PagamentoEntity e1 = PagamentoEntity.builder().id("1").osId(osId).build();
        PagamentoEntity e2 = PagamentoEntity.builder().id("2").osId(osId).build();

        mockScanWithItems(List.of(e1, e2));

        List<PagamentoEntity> result = repository.findByOsId(osId);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve buscar entities por status")
    void deveBuscarEntitiesPorStatus() {
        PagamentoEntity e1 = PagamentoEntity.builder().id("1").status("CONFIRMADO").build();

        mockScanWithItems(List.of(e1));

        List<PagamentoEntity> result = repository.findByStatus("CONFIRMADO");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve verificar existência por orcamentoId e status - existe")
    void deveVerificarExistenciaPorOrcamentoIdEStatusExiste() {
        PagamentoEntity entity = PagamentoEntity.builder()
                .id("1")
                .orcamentoId("orc-1")
                .status("CONFIRMADO")
                .build();

        mockScanWithItems(List.of(entity));

        assertTrue(repository.existsByOrcamentoIdAndStatus("orc-1", "CONFIRMADO"));
    }

    @Test
    @DisplayName("Deve verificar existência por orcamentoId e status - não existe")
    void deveVerificarExistenciaPorOrcamentoIdEStatusNaoExiste() {
        mockScanWithItems(List.of());

        assertFalse(repository.existsByOrcamentoIdAndStatus("orc-1", "CONFIRMADO"));
    }

    @Test
    @DisplayName("Deve deletar entity por ID")
    void deveDeletarEntityPorId() {
        String id = UUID.randomUUID().toString();

        repository.deleteById(id);

        verify(table).deleteItem(any(Key.class));
    }

    @SuppressWarnings("unchecked")
    private void mockScanWithItems(List<PagamentoEntity> items) {
        PageIterable<PagamentoEntity> scanPageIterable = mock(PageIterable.class);
        when(table.scan(any(ScanEnhancedRequest.class))).thenReturn(scanPageIterable);
        when(scanPageIterable.items()).thenReturn(() -> items.iterator());
    }
}
