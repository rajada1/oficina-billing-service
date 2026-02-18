package br.com.grupo99.billingservice.infrastructure.controller;

import br.com.grupo99.billingservice.application.dto.CreateOrcamentoRequest;
import br.com.grupo99.billingservice.application.dto.OrcamentoResponse;
import br.com.grupo99.billingservice.application.service.OrcamentoApplicationService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrcamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrcamentoController - Testes unitários @WebMvcTest")
class OrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrcamentoApplicationService service;

    @MockBean
    private br.com.grupo99.billingservice.infrastructure.security.jwt.JwtUtil jwtUtil;

    private UUID orcamentoId;
    private UUID osId;
    private OrcamentoResponse orcamentoResponse;
    private CreateOrcamentoRequest createRequest;

    @BeforeEach
    void setUp() {
        orcamentoId = UUID.randomUUID();
        osId = UUID.randomUUID();

        orcamentoResponse = OrcamentoResponse.builder()
                .id(orcamentoId)
                .osId(osId)
                .status("PENDENTE")
                .valorTotal(new BigDecimal("500.00"))
                .itens(List.of(
                        OrcamentoResponse.ItemOrcamentoResponse.builder()
                                .descricao("Troca de óleo")
                                .valor(new BigDecimal("100.00"))
                                .quantidade(1)
                                .build()))
                .observacao("Revisão completa")
                .dataGeracao(Instant.now())
                .historicoCount(1)
                .build();

        createRequest = CreateOrcamentoRequest.builder()
                .osId(osId)
                .itens(List.of(
                        CreateOrcamentoRequest.ItemOrcamentoRequest.builder()
                                .descricao("Troca de óleo")
                                .valor(new BigDecimal("100.00"))
                                .quantidade(1)
                                .build()))
                .observacao("Revisão completa")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/orcamentos")
    class CriarOrcamento {

        @Test
        @DisplayName("Deve criar orçamento com status 201")
        void deveCriarOrcamentoComSucesso() throws Exception {
            when(service.criar(any(CreateOrcamentoRequest.class))).thenReturn(orcamentoResponse);

            mockMvc.perform(post("/api/v1/orcamentos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orcamentoId.toString()))
                    .andExpect(jsonPath("$.osId").value(osId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));

            verify(service).criar(any(CreateOrcamentoRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 500 quando serviço lança exceção")
        void deveRetornar500QuandoErro() throws Exception {
            when(service.criar(any(CreateOrcamentoRequest.class)))
                    .thenThrow(new RuntimeException("Erro ao criar"));

            mockMvc.perform(post("/api/v1/orcamentos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orcamentos/{id}")
    class ObterPorId {

        @Test
        @DisplayName("Deve obter orçamento por ID com sucesso")
        void deveObterOrcamentoComSucesso() throws Exception {
            when(service.obterPorId(orcamentoId)).thenReturn(orcamentoResponse);

            mockMvc.perform(get("/api/v1/orcamentos/{id}", orcamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orcamentoId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));

            verify(service).obterPorId(orcamentoId);
        }

        @Test
        @DisplayName("Deve retornar 500 quando não encontrado")
        void deveRetornar500QuandoNaoEncontrado() throws Exception {
            when(service.obterPorId(orcamentoId))
                    .thenThrow(new RuntimeException("Orçamento não encontrado"));

            mockMvc.perform(get("/api/v1/orcamentos/{id}", orcamentoId))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orcamentos/{id}/aprovar")
    class AprovarOrcamento {

        @Test
        @DisplayName("Deve aprovar orçamento com sucesso")
        void deveAprovarOrcamentoComSucesso() throws Exception {
            OrcamentoResponse aprovado = OrcamentoResponse.builder()
                    .id(orcamentoId)
                    .osId(osId)
                    .status("APROVADO")
                    .build();

            when(service.aprovar(orcamentoId)).thenReturn(aprovado);

            mockMvc.perform(put("/api/v1/orcamentos/{id}/aprovar", orcamentoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"));

            verify(service).aprovar(orcamentoId);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orcamentos/{id}/rejeitar")
    class RejeitarOrcamento {

        @Test
        @DisplayName("Deve rejeitar orçamento com sucesso")
        void deveRejeitarOrcamentoComSucesso() throws Exception {
            OrcamentoResponse rejeitado = OrcamentoResponse.builder()
                    .id(orcamentoId)
                    .osId(osId)
                    .status("REJEITADO")
                    .build();

            when(service.rejeitar(eq(orcamentoId), any())).thenReturn(rejeitado);

            mockMvc.perform(put("/api/v1/orcamentos/{id}/rejeitar", orcamentoId)
                    .param("motivo", "Preço alto"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJEITADO"));

            verify(service).rejeitar(eq(orcamentoId), eq("Preço alto"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/orcamentos/{id}")
    class CancelarOrcamento {

        @Test
        @DisplayName("Deve cancelar orçamento com sucesso")
        void deveCancelarOrcamentoComSucesso() throws Exception {
            doNothing().when(service).cancelar(orcamentoId);

            mockMvc.perform(delete("/api/v1/orcamentos/{id}", orcamentoId))
                    .andExpect(status().isNoContent());

            verify(service).cancelar(orcamentoId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orcamentos")
    class ListarTodos {

        @Test
        @DisplayName("Deve listar todos os orçamentos com sucesso")
        void deveListarTodosComSucesso() throws Exception {
            when(service.listarTodos()).thenReturn(List.of(orcamentoResponse));

            mockMvc.perform(get("/api/v1/orcamentos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(orcamentoId.toString()));

            verify(service).listarTodos();
        }
    }
}
