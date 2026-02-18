package br.com.grupo99.billingservice.application.mapper;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.domain.model.ItemOrcamento;
import br.com.grupo99.billingservice.domain.model.Orcamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrcamentoMapper - Testes unitários")
class OrcamentoMapperTest {

    private OrcamentoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrcamentoMapper();
    }

    @Test
    @DisplayName("Deve converter CreateOrcamentoRequest para Orcamento domain")
    void deveConverterRequestParaDomain() {
        UUID osId = UUID.randomUUID();
        CreateOrcamentoRequest request = CreateOrcamentoRequest.builder()
                .osId(osId)
                .itens(List.of(
                        CreateOrcamentoRequest.ItemOrcamentoRequest.builder()
                                .descricao("Troca de óleo")
                                .valor(new BigDecimal("50.00"))
                                .quantidade(2)
                                .build(),
                        CreateOrcamentoRequest.ItemOrcamentoRequest.builder()
                                .descricao("Filtro")
                                .valor(new BigDecimal("30.00"))
                                .quantidade(1)
                                .build()
                ))
                .observacao("Revisão completa")
                .build();

        Orcamento result = mapper.toDomain(request);

        assertThat(result).isNotNull();
        assertThat(result.getOsId()).isEqualTo(osId);
        assertThat(result.getItens()).hasSize(2);
        assertThat(result.getObservacao()).isEqualTo("Revisão completa");
        assertThat(result.getValorTotal()).isEqualByComparingTo(new BigDecimal("130.00"));
    }

    @Test
    @DisplayName("Deve retornar null quando request é null")
    void deveRetornarNullQuandoRequestNull() {
        Orcamento result = mapper.toDomain(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve converter request sem itens")
    void deveConverterRequestSemItens() {
        UUID osId = UUID.randomUUID();
        CreateOrcamentoRequest request = CreateOrcamentoRequest.builder()
                .osId(osId)
                .itens(null)
                .build();

        Orcamento result = mapper.toDomain(request);

        assertThat(result).isNotNull();
        assertThat(result.getOsId()).isEqualTo(osId);
    }

    @Test
    @DisplayName("Deve converter request sem observação")
    void deveConverterRequestSemObservacao() {
        UUID osId = UUID.randomUUID();
        CreateOrcamentoRequest request = CreateOrcamentoRequest.builder()
                .osId(osId)
                .itens(List.of())
                .observacao(null)
                .build();

        Orcamento result = mapper.toDomain(request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Deve converter Orcamento domain para OrcamentoResponse")
    void deveConverterDomainParaResponse() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = Orcamento.criar(osId, List.of(
                ItemOrcamento.builder()
                        .descricao("Troca de óleo")
                        .valorUnitario(new BigDecimal("50.00"))
                        .valorTotal(new BigDecimal("100.00"))
                        .quantidade(2)
                        .build()
        ));

        OrcamentoResponse result = mapper.toResponse(orcamento);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orcamento.getId());
        assertThat(result.getOsId()).isEqualTo(osId);
        assertThat(result.getStatus()).isEqualTo("PENDENTE");
        assertThat(result.getItens()).hasSize(1);
        assertThat(result.getItens().get(0).getDescricao()).isEqualTo("Troca de óleo");
        assertThat(result.getHistoricoCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar null quando orcamento é null")
    void deveRetornarNullQuandoOrcamentoNull() {
        OrcamentoResponse result = mapper.toResponse(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve converter orcamento sem itens para response")
    void deveConverterOrcamentoSemItensParaResponse() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = Orcamento.criar(osId, List.of());

        OrcamentoResponse result = mapper.toResponse(orcamento);

        assertThat(result).isNotNull();
        assertThat(result.getItens()).isEmpty();
    }
}
