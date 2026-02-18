package br.com.grupo99.billingservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HistoricoStatus (Billing) - Testes Unitários")
class HistoricoStatusTest {

    @Test
    @DisplayName("Deve criar HistoricoStatus via factory method")
    void deveCriarHistoricoViaFactoryMethod() {
        HistoricoStatus historico = HistoricoStatus.criar(
                StatusOrcamento.PENDENTE, StatusOrcamento.APROVADO, "admin", "Cliente aprovou"
        );

        assertThat(historico.getStatusAnterior()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(historico.getNovoStatus()).isEqualTo(StatusOrcamento.APROVADO);
        assertThat(historico.getUsuario()).isEqualTo("admin");
        assertThat(historico.getObservacao()).isEqualTo("Cliente aprovou");
        assertThat(historico.getData()).isNotNull();
    }

    @Test
    @DisplayName("Deve criar com construtor padrão")
    void deveCriarComConstrutorPadrao() {
        HistoricoStatus historico = new HistoricoStatus();

        assertThat(historico.getStatusAnterior()).isNull();
        assertThat(historico.getNovoStatus()).isNull();
    }

    @Test
    @DisplayName("Deve criar com todos os argumentos")
    void deveCriarComTodosOsArgumentos() {
        Instant agora = Instant.now();
        HistoricoStatus historico = new HistoricoStatus(
                StatusOrcamento.PENDENTE, StatusOrcamento.REJEITADO, agora, "user", "Obs"
        );

        assertThat(historico.getStatusAnterior()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(historico.getNovoStatus()).isEqualTo(StatusOrcamento.REJEITADO);
        assertThat(historico.getData()).isEqualTo(agora);
    }

    @Test
    @DisplayName("Deve retornar statusNovo via getStatusNovo")
    void deveRetornarStatusNovo() {
        HistoricoStatus historico = HistoricoStatus.criar(
                StatusOrcamento.PENDENTE, StatusOrcamento.APROVADO, "user", "Obs"
        );

        assertThat(historico.getStatusNovo()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    @DisplayName("Deve retornar dataTransicao via getDataTransicao")
    void deveRetornarDataTransicao() {
        HistoricoStatus historico = HistoricoStatus.criar(
                StatusOrcamento.PENDENTE, StatusOrcamento.APROVADO, "user", "Obs"
        );

        assertThat(historico.getDataTransicao()).isNotNull();
        assertThat(historico.getDataTransicao()).isEqualTo(historico.getData());
    }

    @Test
    @DisplayName("Deve usar builder corretamente")
    void deveUsarBuilderCorretamente() {
        Instant agora = Instant.now();
        HistoricoStatus historico = HistoricoStatus.builder()
                .statusAnterior(StatusOrcamento.PENDENTE)
                .novoStatus(StatusOrcamento.CANCELADO)
                .data(agora)
                .usuario("sistema")
                .observacao("Cancelamento automático")
                .build();

        assertThat(historico.getStatusAnterior()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(historico.getNovoStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(historico.getUsuario()).isEqualTo("sistema");
    }
}
