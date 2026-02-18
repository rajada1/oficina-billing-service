package br.com.grupo99.billingservice.infrastructure.controller;

import br.com.grupo99.billingservice.application.dto.CreatePagamentoRequest;
import br.com.grupo99.billingservice.application.dto.PagamentoResponse;
import br.com.grupo99.billingservice.application.service.PagamentoApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PagamentoController - Testes unitários @WebMvcTest")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagamentoApplicationService service;

    @MockBean
    private br.com.grupo99.billingservice.infrastructure.security.jwt.JwtUtil jwtUtil;

    private UUID pagamentoId;
    private UUID orcamentoId;
    private UUID osId;
    private PagamentoResponse pagamentoResponse;
    private CreatePagamentoRequest createRequest;

    @BeforeEach
    void setUp() {
        pagamentoId = UUID.randomUUID();
        orcamentoId = UUID.randomUUID();
        osId = UUID.randomUUID();

        pagamentoResponse = PagamentoResponse.builder()
                .id(pagamentoId)
                .orcamentoId(orcamentoId)
                .osId(osId)
                .status("PENDENTE")
                .valor(new BigDecimal("500.00"))
                .formaPagamento("PIX")
                .initPoint("https://mp.com/pay/123")
                .build();

        createRequest = CreatePagamentoRequest.builder()
                .orcamentoId(orcamentoId)
                .osId(osId)
                .valor(new BigDecimal("500.00"))
                .formaPagamento("PIX")
                .payerEmail("test@test.com")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/pagamentos")
    class RegistrarPagamento {

        @Test
        @DisplayName("Deve registrar pagamento com status 201")
        void deveRegistrarPagamentoComSucesso() throws Exception {
            when(service.registrar(any(CreatePagamentoRequest.class))).thenReturn(pagamentoResponse);

            mockMvc.perform(post("/api/v1/pagamentos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(pagamentoId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(jsonPath("$.initPoint").value("https://mp.com/pay/123"));

            verify(service).registrar(any(CreatePagamentoRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 500 com mensagem de erro quando serviço falha")
        void deveRetornar500QuandoErro() throws Exception {
            when(service.registrar(any(CreatePagamentoRequest.class)))
                    .thenThrow(new RuntimeException("Erro ao registrar pagamento"));

            mockMvc.perform(post("/api/v1/pagamentos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Erro ao registrar pagamento"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/pagamentos")
    class ListarTodos {

        @Test
        @DisplayName("Deve listar todos os pagamentos")
        void deveListarTodosComSucesso() throws Exception {
            when(service.listarTodos()).thenReturn(List.of(pagamentoResponse));

            mockMvc.perform(get("/api/v1/pagamentos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(pagamentoId.toString()));

            verify(service).listarTodos();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/pagamentos/{id}/checar")
    class ChecarPagamento {

        @Test
        @DisplayName("Deve checar pagamento com sucesso")
        void deveChecarPagamentoComSucesso() throws Exception {
            when(service.checarPagamento(pagamentoId)).thenReturn(pagamentoResponse);

            mockMvc.perform(get("/api/v1/pagamentos/{id}/checar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(pagamentoId.toString()));

            verify(service).checarPagamento(pagamentoId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/pagamentos/webhook")
    class Webhook {

        @Test
        @DisplayName("Deve processar webhook de pagamento com sucesso")
        void deveProcessarWebhookComSucesso() throws Exception {
            Map<String, Object> payload = Map.of(
                    "type", "payment",
                    "action", "payment.updated",
                    "data", Map.of("id", "12345"));

            doNothing().when(service).processarWebhook(12345L);

            mockMvc.perform(post("/api/v1/pagamentos/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            verify(service).processarWebhook(12345L);
        }

        @Test
        @DisplayName("Deve ignorar webhook de tipo diferente de payment")
        void deveIgnorarWebhookNaoPayment() throws Exception {
            Map<String, Object> payload = Map.of(
                    "type", "plan",
                    "action", "plan.created");

            mockMvc.perform(post("/api/v1/pagamentos/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            verify(service, never()).processarWebhook(any());
        }

        @Test
        @DisplayName("Deve retornar 200 mesmo quando ocorre erro (evitar reenvio do MP)")
        void deveRetornar200MesmoComErro() throws Exception {
            Map<String, Object> payload = Map.of(
                    "type", "payment",
                    "action", "payment.updated",
                    "data", Map.of("id", "12345"));

            doThrow(new RuntimeException("Erro interno")).when(service).processarWebhook(12345L);

            mockMvc.perform(post("/api/v1/pagamentos/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/pagamentos/{id}/confirmar")
    class ConfirmarPagamento {

        @Test
        @DisplayName("Deve confirmar pagamento com sucesso")
        void deveConfirmarPagamentoComSucesso() throws Exception {
            PagamentoResponse confirmado = PagamentoResponse.builder()
                    .id(pagamentoId)
                    .status("CONFIRMADO")
                    .build();

            when(service.confirmar(pagamentoId)).thenReturn(confirmado);

            mockMvc.perform(put("/api/v1/pagamentos/{id}/confirmar", pagamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMADO"));

            verify(service).confirmar(pagamentoId);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/pagamentos/{id}/estornar")
    class EstornarPagamento {

        @Test
        @DisplayName("Deve estornar pagamento com sucesso")
        void deveEstornarPagamentoComSucesso() throws Exception {
            PagamentoResponse estornado = PagamentoResponse.builder()
                    .id(pagamentoId)
                    .status("ESTORNADO")
                    .build();

            when(service.estornar(eq(pagamentoId), any())).thenReturn(estornado);

            mockMvc.perform(put("/api/v1/pagamentos/{id}/estornar", pagamentoId)
                    .param("motivo", "Erro no serviço"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ESTORNADO"));

            verify(service).estornar(eq(pagamentoId), eq("Erro no serviço"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/pagamentos/{id}")
    class CancelarPagamento {

        @Test
        @DisplayName("Deve cancelar pagamento com sucesso")
        void deveCancelarPagamentoComSucesso() throws Exception {
            doNothing().when(service).cancelar(pagamentoId);

            mockMvc.perform(delete("/api/v1/pagamentos/{id}", pagamentoId))
                    .andExpect(status().isNoContent());

            verify(service).cancelar(pagamentoId);
        }
    }
}
