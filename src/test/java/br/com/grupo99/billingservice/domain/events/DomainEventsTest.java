package br.com.grupo99.billingservice.domain.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Events - Testes unitários")
class DomainEventsTest {

    @Nested
    @DisplayName("OSCriadaEvent")
    class OSCriadaEventTests {

        @Test
        @DisplayName("Deve criar evento com todos os campos")
        void deveCriarEventoComTodosCampos() {
            UUID osId = UUID.randomUUID();
            UUID clienteId = UUID.randomUUID();
            UUID veiculoId = UUID.randomUUID();
            LocalDateTime timestamp = LocalDateTime.now();

            OSCriadaEvent event = new OSCriadaEvent(osId, clienteId, veiculoId, "Revisão", timestamp, "OS_CRIADA");

            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getClienteId()).isEqualTo(clienteId);
            assertThat(event.getVeiculoId()).isEqualTo(veiculoId);
            assertThat(event.getDescricao()).isEqualTo("Revisão");
            assertThat(event.getTimestamp()).isEqualTo(timestamp);
            assertThat(event.getEventType()).isEqualTo("OS_CRIADA");
        }

        @Test
        @DisplayName("Deve criar evento vazio e popular via setters")
        void deveCriarEventoVazio() {
            OSCriadaEvent event = new OSCriadaEvent();
            UUID osId = UUID.randomUUID();
            event.setOsId(osId);
            event.setDescricao("Teste");

            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getDescricao()).isEqualTo("Teste");
        }
    }

    @Nested
    @DisplayName("OrcamentoAprovadoEvent")
    class OrcamentoAprovadoEventTests {

        @Test
        @DisplayName("Deve criar evento via builder")
        void deveCriarEventoViaBuilder() {
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();

            OrcamentoAprovadoEvent event = OrcamentoAprovadoEvent.builder()
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .valorTotal(new BigDecimal("1000.00"))
                    .aprovadoPor("admin")
                    .timestamp(LocalDateTime.now())
                    .build();

            assertThat(event.getOrcamentoId()).isEqualTo(orcamentoId);
            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getValorTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(event.getAprovadoPor()).isEqualTo("admin");
            assertThat(event.getEventType()).isEqualTo("ORCAMENTO_APROVADO");
        }

        @Test
        @DisplayName("Deve ter eventType padrão ORCAMENTO_APROVADO")
        void deveTermEventTypePadrao() {
            OrcamentoAprovadoEvent event = OrcamentoAprovadoEvent.builder().build();
            assertThat(event.getEventType()).isEqualTo("ORCAMENTO_APROVADO");
        }
    }

    @Nested
    @DisplayName("OrcamentoProntoEvent")
    class OrcamentoProntoEventTests {

        @Test
        @DisplayName("Deve criar evento via builder")
        void deveCriarEventoViaBuilder() {
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();

            OrcamentoProntoEvent event = OrcamentoProntoEvent.builder()
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .valorTotal(new BigDecimal("500.00"))
                    .timestamp(LocalDateTime.now())
                    .build();

            assertThat(event.getOrcamentoId()).isEqualTo(orcamentoId);
            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getValorTotal()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(event.getEventType()).isEqualTo("ORCAMENTO_PRONTO");
            assertThat(event.getPrazoValidadeDias()).isEqualTo(7);
        }

        @Test
        @DisplayName("Deve ter prazo padrão de 7 dias")
        void deveTermPrazoPadrao() {
            OrcamentoProntoEvent event = OrcamentoProntoEvent.builder().build();
            assertThat(event.getPrazoValidadeDias()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("OrcamentoRejeitadoEvent")
    class OrcamentoRejeitadoEventTests {

        @Test
        @DisplayName("Deve criar evento via builder")
        void deveCriarEventoViaBuilder() {
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();

            OrcamentoRejeitadoEvent event = OrcamentoRejeitadoEvent.builder()
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .motivo("Preço alto")
                    .rejeitadoPor("cliente")
                    .timestamp(LocalDateTime.now())
                    .build();

            assertThat(event.getOrcamentoId()).isEqualTo(orcamentoId);
            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getMotivo()).isEqualTo("Preço alto");
            assertThat(event.getRejeitadoPor()).isEqualTo("cliente");
            assertThat(event.getEventType()).isEqualTo("ORCAMENTO_REJEITADO");
        }
    }

    @Nested
    @DisplayName("DiagnosticoConcluidoEvent")
    class DiagnosticoConcluidoEventTests {

        @Test
        @DisplayName("Deve criar evento com todos os campos")
        void deveCriarEventoComTodosCampos() {
            UUID osId = UUID.randomUUID();
            UUID execucaoId = UUID.randomUUID();

            DiagnosticoConcluidoEvent event = new DiagnosticoConcluidoEvent(
                    osId, execucaoId, "Motor saudável",
                    new BigDecimal("200.00"), LocalDateTime.now(), "DIAGNOSTICO_CONCLUIDO");

            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getExecucaoId()).isEqualTo(execucaoId);
            assertThat(event.getDiagnostico()).isEqualTo("Motor saudável");
            assertThat(event.getValorEstimado()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(event.getEventType()).isEqualTo("DIAGNOSTICO_CONCLUIDO");
        }
    }

    @Nested
    @DisplayName("PagamentoFalhouEvent")
    class PagamentoFalhouEventTests {

        @Test
        @DisplayName("Deve criar evento via builder")
        void deveCriarEventoViaBuilder() {
            UUID pagamentoId = UUID.randomUUID();
            UUID orcamentoId = UUID.randomUUID();
            UUID osId = UUID.randomUUID();

            PagamentoFalhouEvent event = PagamentoFalhouEvent.builder()
                    .pagamentoId(pagamentoId)
                    .orcamentoId(orcamentoId)
                    .osId(osId)
                    .motivo("Saldo insuficiente")
                    .mensagemErro("cc_rejected_insufficient_amount")
                    .codigoErro("ERR-001")
                    .valorTentado(new BigDecimal("500.00"))
                    .timestamp(LocalDateTime.now())
                    .build();

            assertThat(event.getPagamentoId()).isEqualTo(pagamentoId);
            assertThat(event.getOrcamentoId()).isEqualTo(orcamentoId);
            assertThat(event.getOsId()).isEqualTo(osId);
            assertThat(event.getMotivo()).isEqualTo("Saldo insuficiente");
            assertThat(event.getMensagemErro()).isEqualTo("cc_rejected_insufficient_amount");
            assertThat(event.getCodigoErro()).isEqualTo("ERR-001");
            assertThat(event.getValorTentado()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(event.getEventType()).isEqualTo("PAGAMENTO_FALHOU");
        }

        @Test
        @DisplayName("Deve ter eventType padrão PAGAMENTO_FALHOU")
        void deveTermEventTypePadrao() {
            PagamentoFalhouEvent event = PagamentoFalhouEvent.builder().build();
            assertThat(event.getEventType()).isEqualTo("PAGAMENTO_FALHOU");
        }
    }
}
