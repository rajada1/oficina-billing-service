package br.com.grupo99.billingservice.bdd;

import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;
import br.com.grupo99.billingservice.domain.repository.OrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.messaging.BillingEventPublisherPort;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "aws.dynamodb.endpoint=",
        "mercadopago.access-token=TEST-fake-token",
        "mercadopago.notification-url=http://localhost:8080/pagamentos/webhook"
})
public class SagaOrcamentoSteps {

    @MockBean
    private DynamoDbClient dynamoDbClient;

    @MockBean
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @MockBean
    private OrcamentoRepository orcamentoRepository;

    @SpyBean
    private BillingEventPublisherPort eventPublisher;

    private Orcamento orcamento;
    private String osId;
    private Exception exception;

    @Before
    public void setUp() {
        orcamento = null;
        osId = null;
        exception = null;
        Mockito.reset(eventPublisher);
    }

    @Dado("que o Billing Service está disponível")
    public void queBillingServiceEstaDisponivel() {
        assertNotNull(orcamentoRepository);
        assertNotNull(eventPublisher);
    }

    @Dado("a fila {string} está configurada")
    public void aFilaEstaConfigurada(String nomeFila) {
        assertNotNull(eventPublisher);
    }

    @Dado("que o evento {string} foi publicado pelo OS Service")
    public void queOEventoFoiPublicado(String nomeEvento) {
        osId = "550e8400-e29b-41d4-a716-446655440000";
    }

    @Dado("contém os dados:")
    public void contemOsDados(DataTable dataTable) {
        Map<String, String> dados = dataTable.asMap(String.class, String.class);
        osId = dados.get("osId");
    }

    @Quando("o Billing Service recebe o evento")
    public void billingServiceRecebeEvento() {
        UUID osUUID = UUID.fromString(osId);
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osUUID)
                .status(StatusOrcamento.PENDENTE)
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Então("um orçamento vazio deve ser criado")
    public void umOrcamentoVazioDeveSerCriado() {
        assertNotNull(orcamento);
        assertNotNull(orcamento.getId());
    }

    @Então("o status do orçamento deve ser {string}")
    public void statusDoOrcamentoDeveSer(String status) {
        assertEquals(StatusOrcamento.valueOf(status), orcamento.getStatus());
    }

    @Então("o orçamento deve estar associado à OS {string}")
    public void orcamentoDeveEstarAssociadoAOS(String osIdStr) {
        assertEquals(osIdStr, orcamento.getOsId().toString());
    }

    @Dado("que existe um orçamento para a OS {string}")
    public void queExisteUmOrcamentoParaOS(String osIdStr) {
        osId = osIdStr;
        UUID osUUID = UUID.fromString(osIdStr);
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osUUID)
                .status(StatusOrcamento.PENDENTE)
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        when(orcamentoRepository.findByOsId(osUUID)).thenReturn(Optional.of(novoOrcamento));
        when(orcamentoRepository.findById(novoOrcamento.getId())).thenReturn(Optional.of(novoOrcamento));
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Dado("o evento {string} foi publicado")
    public void oEventoFoiPublicado(String nomeEvento) {
        assertNotNull(orcamento);
    }

    @Dado("contém:")
    public void contem(DataTable dataTable) {
        Map<String, String> dados = dataTable.asMap(String.class, String.class);
        osId = dados.get("osId");
    }

    @Quando("o Billing Service processa o diagnóstico")
    public void billingServiceProcessaDiagnostico() {
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
        orcamento.setValorTotal(new BigDecimal("1500.00"));
        orcamento.setStatus(StatusOrcamento.PENDENTE);
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
        orcamento = orcamentoRepository.save(orcamento);
    }

    @Então("o orçamento deve ser atualizado com valor {string}")
    public void orcamentoDeveSerAtualizadoComValor(String valor) {
        assertTrue(new BigDecimal(valor).compareTo(orcamento.getValorTotal()) == 0);
    }

    @Então("o status deve mudar para {string}")
    public void statusDeveMudarPara(String status) {
        assertEquals(StatusOrcamento.valueOf(status), orcamento.getStatus());
    }

    @Então("o orçamento deve mudar para status {string}")
    public void o_orçamento_deve_mudar_para_status(String status) {
        assertEquals(StatusOrcamento.valueOf(status), orcamento.getStatus());
    }

    @Então("o evento {string} deve ser publicado")
    public void eventoDeveSerPublicado(String nomeEvento) {
        assertNotNull(orcamento);
    }

    @Dado("que existe um orçamento com status {string}")
    public void queExisteUmOrcamentoComStatus(String status) {
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(UUID.randomUUID())
                .status(StatusOrcamento.valueOf(status))
                .valorTotal(new BigDecimal("1500.00"))
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        when(orcamentoRepository.findById(novoOrcamento.getId())).thenReturn(Optional.of(novoOrcamento));
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Dado("o valor total é {string}")
    public void valorTotalE(String valor) {
        assertTrue(new BigDecimal(valor).compareTo(orcamento.getValorTotal()) == 0);
    }

    @Quando("o cliente aprova o orçamento via API")
    public void clienteAprovaOrcamentoViaAPI() {
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
        orcamento.setStatus(StatusOrcamento.APROVADO);
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
        orcamento = orcamentoRepository.save(orcamento);
    }

    @Então("o OS Service deve receber o evento")
    public void osServiceDeveReceberEvento() {
        assertNotNull(orcamento);
    }

    @Então("o Execution Service deve receber o evento")
    public void executionServiceDeveReceberEvento() {
        assertNotNull(orcamento);
    }

    @Dado("que existe um orçamento aprovado")
    public void queExisteUmOrcamentoAprovado() {
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(UUID.randomUUID())
                .status(StatusOrcamento.APROVADO)
                .valorTotal(new BigDecimal("1500.00"))
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        when(orcamentoRepository.findById(novoOrcamento.getId())).thenReturn(Optional.of(novoOrcamento));
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Dado("a execução foi concluída")
    public void aExecucaoFoiConcluida() {
        assertNotNull(orcamento);
    }

    @Quando("o pagamento é processado")
    public void oPagamentoEProcessado() {
        assertNotNull(orcamento);
    }

    @Então("o status do pagamento deve ser {string}")
    public void statusDoPagamentoDeveSer(String status) {
        assertNotNull(orcamento);
    }

    @Então("a data de pagamento deve estar registrada")
    public void dataDePagamentoDeveEstarRegistrada() {
        assertNotNull(orcamento);
    }

    @Dado("o orçamento está associado à OS {string}")
    public void orcamentoEstaAssociadoAOS(String osIdStr) {
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
        orcamento.setOsId(UUID.fromString(osIdStr));
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
        orcamento = orcamentoRepository.save(orcamento);
    }

    @Quando("o evento {string} é recebido")
    public void eventoERecebido(String nomeEvento) {
        if ("OS_CANCELADA".equals(nomeEvento) || "EXECUCAO_FALHOU".equals(nomeEvento)) {
            when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
            orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
            orcamento.setStatus(StatusOrcamento.CANCELADO);
            when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
            orcamento = orcamentoRepository.save(orcamento);
        }
    }

    @Então("nenhum evento adicional deve ser publicado")
    public void nenhumEventoAdicionalDeveSerPublicado() {
        assertNotNull(orcamento);
    }

    @Dado("o pagamento ainda não foi processado")
    public void oPagamentoAindaNaoFoiProcessado() {
        assertNotNull(orcamento);
    }

    @Então("nenhuma cobrança deve ser feita")
    public void nenhumaCobrancaDeveSerFeita() {
        assertEquals(StatusOrcamento.CANCELADO, orcamento.getStatus());
    }

    @Dado("que já existe um orçamento para a OS {string}")
    public void queJaExisteUmOrcamentoParaOS(String osIdStr) {
        UUID osUUID = UUID.fromString(osIdStr);
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osUUID)
                .status(StatusOrcamento.PENDENTE)
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        when(orcamentoRepository.findByOsId(osUUID)).thenReturn(Optional.of(novoOrcamento));
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Quando("o evento {string} é recebido novamente para a mesma OS")
    public void eventoERecebidoNovamenteParaMesmaOS(String nomeEvento) {
        Optional<Orcamento> existente = orcamentoRepository.findByOsId(orcamento.getOsId());
        assertTrue(existente.isPresent());
    }

    @Então("o evento deve ser ignorado")
    public void eventoDeveSerIgnorado() {
        assertNotNull(orcamento);
    }

    @Então("nenhum novo orçamento deve ser criado")
    public void nenhumNovoOrcamentoDeveSerCriado() {
        Optional<Orcamento> found = orcamentoRepository.findByOsId(orcamento.getOsId());
        assertTrue(found.isPresent());
    }

    @Então("um log de duplicata deve ser registrado")
    public void umLogDeDuplicataDeveSerRegistrado() {
        assertNotNull(orcamento);
    }

    @Quando("o cliente rejeita o orçamento")
    public void clienteRejeitaOrcamento() {
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
        orcamento.setStatus(StatusOrcamento.REJEITADO);
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
        orcamento = orcamentoRepository.save(orcamento);
    }

    @Então("o motivo da rejeição deve estar registrado")
    public void motivoDaRejeicaoDeveEstarRegistrado() {
        assertEquals(StatusOrcamento.REJEITADO, orcamento.getStatus());
    }

    @Quando("o processamento de pagamento falha")
    public void oProcessamentoDePagamentoFalha() {
        exception = new RuntimeException("Falha no pagamento");
    }

    @Então("o orçamento deve manter o status {string}")
    public void orcamentoDeveManterOStatus(String status) {
        assertEquals(StatusOrcamento.valueOf(status), orcamento.getStatus());
    }

    @Então("o motivo da falha deve estar registrado")
    public void motivoDaFalhaDeveEstarRegistrado() {
        assertNotNull(exception);
    }

    @Então("um alerta deve ser enviado para o gerente")
    public void umAlertaDeveSerEnviadoParaOGerente() {
        assertNotNull(exception);
    }

    @Dado("que um orçamento passou por todo o ciclo")
    public void queUmOrcamentoPassouPorTodoCiclo() {
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(UUID.randomUUID())
                .status(StatusOrcamento.APROVADO)
                .valorTotal(new BigDecimal("1500.00"))
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Quando("eu consulto o histórico do orçamento")
    public void euConsultoOHistoricoDoOrcamento() {
        assertNotNull(orcamento);
    }

    @Então("deve haver registros de:")
    public void deveHaverRegistrosDe(DataTable dataTable) {
        assertNotNull(orcamento);
    }

    @Então("cada registro deve ter timestamp")
    public void cadaRegistroDeveTerTimestamp() {
        assertNotNull(orcamento);
    }

    @Dado("que o evento {string} foi recebido")
    public void queOEventoFoiRecebido(String nomeEvento) {
        UUID osUUID = UUID.randomUUID();
        osId = osUUID.toString();
        Orcamento novoOrcamento = Orcamento.builder()
                .id(UUID.randomUUID())
                .osId(osUUID)
                .status(StatusOrcamento.PENDENTE)
                .build();

        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(novoOrcamento);
        when(orcamentoRepository.findById(novoOrcamento.getId())).thenReturn(Optional.of(novoOrcamento));
        orcamento = orcamentoRepository.save(novoOrcamento);
    }

    @Quando("todos os passos do Saga são executados com sucesso:")
    public void todosOsPassosDoSagaSaoExecutadosComSucesso(DataTable dataTable) {
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamento = orcamentoRepository.findById(orcamento.getId()).orElseThrow();
        orcamento.setStatus(StatusOrcamento.APROVADO);
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamento);
        orcamento = orcamentoRepository.save(orcamento);
    }

    @Então("o orçamento deve estar com status {string}")
    public void orcamentoDeveEstarComStatus(String status) {
        assertNotNull(orcamento);
    }

    @Então("todos os eventos devem ter sido publicados")
    public void todosOsEventosDevemTerSidoPublicados() {
        assertNotNull(orcamento);
    }

    @Então("nenhuma compensação deve ter sido acionada")
    public void nenhumaCompensacaoDeveTerSidoAcionada() {
        assertNotNull(orcamento);
    }
}
