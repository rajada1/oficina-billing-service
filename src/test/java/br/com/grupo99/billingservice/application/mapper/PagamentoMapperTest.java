package br.com.grupo99.billingservice.application.mapper;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.domain.model.FormaPagamento;
import br.com.grupo99.billingservice.domain.model.Pagamento;
import br.com.grupo99.billingservice.domain.model.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagamentoMapper - Testes unitários")
class PagamentoMapperTest {

    private PagamentoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PagamentoMapper();
    }

    @Test
    @DisplayName("Deve converter CreatePagamentoRequest para Pagamento domain")
    void deveConverterRequestParaDomain() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();

        CreatePagamentoRequest request = CreatePagamentoRequest.builder()
                .orcamentoId(orcamentoId)
                .osId(osId)
                .valor(new BigDecimal("500.00"))
                .formaPagamento("PIX")
                .comprovante("COMP-123")
                .build();

        Pagamento result = mapper.toDomain(request);

        assertThat(result).isNotNull();
        assertThat(result.getOrcamentoId()).isEqualTo(orcamentoId);
        assertThat(result.getOsId()).isEqualTo(osId);
        assertThat(result.getValor()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(result.getComprovante()).isEqualTo("COMP-123");
        assertThat(result.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
    }

    @Test
    @DisplayName("Deve retornar null quando request é null")
    void deveRetornarNullQuandoRequestNull() {
        Pagamento result = mapper.toDomain(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve converter Pagamento domain para PagamentoResponse")
    void deveConverterDomainParaResponse() {
        UUID pagamentoId = UUID.randomUUID();
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Instant dataPagamento = Instant.now();

        Pagamento pagamento = Pagamento.builder()
                .id(pagamentoId)
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status(StatusPagamento.CONFIRMADO)
                .valor(new BigDecimal("500.00"))
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .comprovante("COMP-123")
                .mercadoPagoPaymentId(12345L)
                .mercadoPagoPreferenceId("pref-123")
                .initPoint("https://mp.com/pay")
                .dataPagamento(dataPagamento)
                .dataEstorno(null)
                .motivoEstorno(null)
                .build();

        PagamentoResponse result = mapper.toResponse(pagamento);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pagamentoId);
        assertThat(result.getOrcamentoId()).isEqualTo(orcamentoId);
        assertThat(result.getOsId()).isEqualTo(osId);
        assertThat(result.getStatus()).isEqualTo("CONFIRMADO");
        assertThat(result.getValor()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getFormaPagamento()).isEqualTo("CARTAO_CREDITO");
        assertThat(result.getComprovante()).isEqualTo("COMP-123");
        assertThat(result.getMercadoPagoPaymentId()).isEqualTo(12345L);
        assertThat(result.getMercadoPagoPreferenceId()).isEqualTo("pref-123");
        assertThat(result.getInitPoint()).isEqualTo("https://mp.com/pay");
        assertThat(result.getDataPagamento()).isEqualTo(dataPagamento);
    }

    @Test
    @DisplayName("Deve retornar null quando pagamento é null")
    void deveRetornarNullQuandoPagamentoNull() {
        PagamentoResponse result = mapper.toResponse(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve converter pagamento com forma BOLETO")
    void deveConverterRequestComFormaBoleto() {
        CreatePagamentoRequest request = CreatePagamentoRequest.builder()
                .orcamentoId(UUID.randomUUID())
                .osId(UUID.randomUUID())
                .valor(new BigDecimal("300.00"))
                .formaPagamento("BOLETO")
                .build();

        Pagamento result = mapper.toDomain(request);

        assertThat(result).isNotNull();
        assertThat(result.getFormaPagamento()).isEqualTo(FormaPagamento.BOLETO);
    }

    @Test
    @DisplayName("Deve converter pagamento estornado para response")
    void deveConverterPagamentoEstornado() {
        Instant dataEstorno = Instant.now();

        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .orcamentoId(UUID.randomUUID())
                .osId(UUID.randomUUID())
                .status(StatusPagamento.ESTORNADO)
                .valor(new BigDecimal("500.00"))
                .formaPagamento(FormaPagamento.PIX)
                .dataEstorno(dataEstorno)
                .motivoEstorno("Erro no serviço")
                .build();

        PagamentoResponse result = mapper.toResponse(pagamento);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ESTORNADO");
        assertThat(result.getDataEstorno()).isEqualTo(dataEstorno);
        assertThat(result.getMotivoEstorno()).isEqualTo("Erro no serviço");
    }
}
