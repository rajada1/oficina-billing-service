package br.com.grupo99.billingservice.application.service;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.application.mapper.OrcamentoMapper;
import br.com.grupo99.billingservice.domain.model.ItemOrcamento;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;
import br.com.grupo99.billingservice.domain.repository.OrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrcamentoApplicationService - Testes unitários")
class OrcamentoApplicationServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private BillingEventPublisherPort eventPublisher;

    @Mock
    private OrcamentoMapper mapper;

    @InjectMocks
    private OrcamentoApplicationService service;

    private UUID osId;
    private UUID orcamentoId;
    private Orcamento orcamento;
    private OrcamentoResponse orcamentoResponse;
    private CreateOrcamentoRequest createRequest;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        orcamentoId = UUID.randomUUID();

        orcamento = Orcamento.builder()
                .id(orcamentoId)
                .osId(osId)
                .status(StatusOrcamento.PENDENTE)
                .itens(new ArrayList<>())
                .historico(new ArrayList<>())
                .valorTotal(new BigDecimal("500.00"))
                .dataGeracao(Instant.now())
                .build();

        orcamentoResponse = OrcamentoResponse.builder()
                .id(orcamentoId)
                .osId(osId)
                .status("PENDENTE")
                .valorTotal(new BigDecimal("500.00"))
                .build();

        createRequest = CreateOrcamentoRequest.builder()
                .osId(osId)
                .itens(List.of(
                        CreateOrcamentoRequest.ItemOrcamentoRequest.builder()
                                .descricao("Troca de óleo")
                                .valor(new BigDecimal("100.00"))
                                .quantidade(1)
                                .build()
                ))
                .observacao("Revisão completa")
                .build();
    }

    @Nested
    @DisplayName("Criar Orçamento")
    class CriarOrcamento {

        @Test
        @DisplayName("Deve criar orçamento com sucesso")
        void deveCriarOrcamentoComSucesso() {
            when(mapper.toDomain(createRequest)).thenReturn(orcamento);
            when(orcamentoRepository.save(orcamento)).thenReturn(orcamento);
            when(mapper.toResponse(orcamento)).thenReturn(orcamentoResponse);

            OrcamentoResponse result = service.criar(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orcamentoId);
            assertThat(result.getOsId()).isEqualTo(osId);

            verify(mapper).toDomain(createRequest);
            verify(orcamentoRepository).save(orcamento);
            verify(eventPublisher).publicarOrcamentoCriado(orcamento);
            verify(mapper).toResponse(orcamento);
        }
    }

    @Nested
    @DisplayName("Obter por ID")
    class ObterPorId {

        @Test
        @DisplayName("Deve retornar orçamento quando encontrado")
        void deveRetornarOrcamentoQuandoEncontrado() {
            when(orcamentoRepository.findById(orcamentoId)).thenReturn(Optional.of(orcamento));
            when(mapper.toResponse(orcamento)).thenReturn(orcamentoResponse);

            OrcamentoResponse result = service.obterPorId(orcamentoId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orcamentoId);
            verify(orcamentoRepository).findById(orcamentoId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(orcamentoRepository.findById(orcamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obterPorId(orcamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Orçamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Aprovar Orçamento")
    class AprovarOrcamento {

        @Test
        @DisplayName("Deve aprovar orçamento com sucesso")
        void deveAprovarOrcamentoComSucesso() {
            Orcamento orcamentoPendente = Orcamento.criar(osId, List.of(
                    ItemOrcamento.builder()
                            .descricao("Serviço")
                            .valorUnitario(new BigDecimal("100"))
                            .valorTotal(new BigDecimal("100"))
                            .quantidade(1)
                            .build()
            ));

            OrcamentoResponse aprovadoResponse = OrcamentoResponse.builder()
                    .id(orcamentoPendente.getId())
                    .osId(osId)
                    .status("APROVADO")
                    .build();

            when(orcamentoRepository.findById(orcamentoPendente.getId()))
                    .thenReturn(Optional.of(orcamentoPendente));
            when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamentoPendente);
            when(mapper.toResponse(any(Orcamento.class))).thenReturn(aprovadoResponse);

            OrcamentoResponse result = service.aprovar(orcamentoPendente.getId());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("APROVADO");
            verify(eventPublisher).publicarOrcamentoAprovado(any(Orcamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando orçamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(orcamentoRepository.findById(orcamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.aprovar(orcamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Orçamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Rejeitar Orçamento")
    class RejeitarOrcamento {

        @Test
        @DisplayName("Deve rejeitar orçamento com sucesso")
        void deveRejeitarOrcamentoComSucesso() {
            Orcamento orcamentoPendente = Orcamento.criar(osId, List.of(
                    ItemOrcamento.builder()
                            .descricao("Serviço")
                            .valorUnitario(new BigDecimal("100"))
                            .valorTotal(new BigDecimal("100"))
                            .quantidade(1)
                            .build()
            ));

            OrcamentoResponse rejeitadoResponse = OrcamentoResponse.builder()
                    .id(orcamentoPendente.getId())
                    .osId(osId)
                    .status("REJEITADO")
                    .build();

            when(orcamentoRepository.findById(orcamentoPendente.getId()))
                    .thenReturn(Optional.of(orcamentoPendente));
            when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamentoPendente);
            when(mapper.toResponse(any(Orcamento.class))).thenReturn(rejeitadoResponse);

            OrcamentoResponse result = service.rejeitar(orcamentoPendente.getId(), "Preço alto");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("REJEITADO");
            verify(eventPublisher).publicarOrcamentoRejeitado(any(Orcamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando orçamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(orcamentoRepository.findById(orcamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejeitar(orcamentoId, "Motivo"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Orçamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Cancelar Orçamento")
    class CancelarOrcamento {

        @Test
        @DisplayName("Deve cancelar orçamento com sucesso")
        void deveCancelarOrcamentoComSucesso() {
            Orcamento orcamentoPendente = Orcamento.criar(osId, List.of(
                    ItemOrcamento.builder()
                            .descricao("Serviço")
                            .valorUnitario(new BigDecimal("100"))
                            .valorTotal(new BigDecimal("100"))
                            .quantidade(1)
                            .build()
            ));

            when(orcamentoRepository.findById(orcamentoPendente.getId()))
                    .thenReturn(Optional.of(orcamentoPendente));
            when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamentoPendente);

            service.cancelar(orcamentoPendente.getId());

            verify(orcamentoRepository).save(any(Orcamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando orçamento não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(orcamentoRepository.findById(orcamentoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelar(orcamentoId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Orçamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Cancelar por OS")
    class CancelarPorOs {

        @Test
        @DisplayName("Deve cancelar orçamento por OS ID com sucesso")
        void deveCancelarOrcamentoPorOsComSucesso() {
            Orcamento orcamentoPendente = Orcamento.criar(osId, List.of(
                    ItemOrcamento.builder()
                            .descricao("Serviço")
                            .valorUnitario(new BigDecimal("100"))
                            .valorTotal(new BigDecimal("100"))
                            .quantidade(1)
                            .build()
            ));

            when(orcamentoRepository.findByOsId(osId)).thenReturn(Optional.of(orcamentoPendente));
            when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamentoPendente);

            service.cancelarPorOs(osId, "OS cancelada");

            verify(orcamentoRepository).save(any(Orcamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando orçamento por OS não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(orcamentoRepository.findByOsId(osId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelarPorOs(osId, "Motivo"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Orçamento não encontrado para OS");
        }
    }

    @Nested
    @DisplayName("Listar Todos")
    class ListarTodos {

        @Test
        @DisplayName("Deve listar todos os orçamentos")
        void deveListarTodosOsOrcamentos() {
            when(orcamentoRepository.findAll()).thenReturn(List.of(orcamento));
            when(mapper.toResponse(orcamento)).thenReturn(orcamentoResponse);

            List<OrcamentoResponse> result = service.listarTodos();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(orcamentoId);
            verify(orcamentoRepository).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há orçamentos")
        void deveRetornarListaVaziaQuandoNaoHaOrcamentos() {
            when(orcamentoRepository.findAll()).thenReturn(List.of());

            List<OrcamentoResponse> result = service.listarTodos();

            assertThat(result).isEmpty();
        }
    }
}
