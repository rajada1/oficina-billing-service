# language: pt
Funcionalidade: Saga Pattern - Orçamento e Pagamento
  Como Billing Service
  Eu quero gerenciar orçamentos e pagamentos
  Para coordenar transações financeiras no fluxo Saga

  Contexto:
    Dado que o Billing Service está disponível
    E a fila "billing-events-queue" está configurada

  Cenário: Saga Step 1 - Receber OS Criada e Criar Orçamento
    Dado que o evento "OS_CRIADA" foi publicado pelo OS Service
    E contém os dados:
      | osId          | 550e8400-e29b-41d4-a716-446655440000 |
      | clienteNome   | Maria Santos                          |
      | veiculoPlaca  | XYZ-5678                              |
    Quando o Billing Service recebe o evento
    Então um orçamento vazio deve ser criado
    E o status do orçamento deve ser "PENDENTE"
    E o orçamento deve estar associado à OS "550e8400-e29b-41d4-a716-446655440000"

  Cenário: Saga Step 2 - Receber Diagnóstico e Calcular Orçamento
    Dado que existe um orçamento para a OS "550e8400-e29b-41d4-a716-446655440000"
    E o evento "DIAGNOSTICO_CONCLUIDO" foi publicado
    E contém:
      | osId            | 550e8400-e29b-41d4-a716-446655440000 |
      | valorEstimado   | 1500.00                               |
      | descricao       | Troca de óleo e filtros               |
    Quando o Billing Service processa o diagnóstico
    Então o orçamento deve ser atualizado com valor "1500.00"
    E o status deve mudar para "PENDENTE"
    E o evento "ORCAMENTO_PRONTO" deve ser publicado

  Cenário: Saga Step 3 - Cliente Aprova Orçamento
    Dado que existe um orçamento com status "PENDENTE"
    E o valor total é "1500.00"
    Quando o cliente aprova o orçamento via API
    Então o status deve mudar para "APROVADO"
    E o evento "ORCAMENTO_APROVADO" deve ser publicado
    E o OS Service deve receber o evento
    E o Execution Service deve receber o evento

  Cenário: Saga Step 4 - Processar Pagamento
    Dado que existe um orçamento aprovado
    E a execução foi concluída
    Quando o pagamento é processado
    Então o status do pagamento deve ser "CONFIRMADO"
    E o evento "PAGAMENTO_CONFIRMADO" deve ser publicado
    E a data de pagamento deve estar registrada

  Cenário: Compensação - OS Cancelada
    Dado que existe um orçamento com status "PENDENTE"
    E o orçamento está associado à OS "550e8400-e29b-41d4-a716-446655440000"
    Quando o evento "OS_CANCELADA" é recebido
    Então o orçamento deve mudar para status "CANCELADO"
    E nenhum evento adicional deve ser publicado

  Cenário: Compensação - Execução Falhou
    Dado que existe um orçamento aprovado
    E o pagamento ainda não foi processado
    Quando o evento "EXECUCAO_FALHOU" é recebido
    Então o orçamento deve mudar para status "CANCELADO"
    E nenhuma cobrança deve ser feita

  Cenário: Idempotência - Evento Duplicado de OS_CRIADA
    Dado que já existe um orçamento para a OS "550e8400-e29b-41d4-a716-446655440000"
    Quando o evento "OS_CRIADA" é recebido novamente para a mesma OS
    Então o evento deve ser ignorado
    E nenhum novo orçamento deve ser criado
    E um log de duplicata deve ser registrado

  Cenário: Validação - Orçamento Rejeitado pelo Cliente
    Dado que existe um orçamento com status "PENDENTE"
    Quando o cliente rejeita o orçamento
    Então o status deve mudar para "REJEITADO"
    E o evento "ORCAMENTO_REJEITADO" deve ser publicado
    E o motivo da rejeição deve estar registrado

  Cenário: Validação - Falha no Pagamento
    Dado que existe um orçamento aprovado
    E a execução foi concluída
    Quando o processamento de pagamento falha
    Então o evento "PAGAMENTO_FALHOU" deve ser publicado
    E o orçamento deve manter o status "APROVADO"
    E o motivo da falha deve estar registrado
    E um alerta deve ser enviado para o gerente

  Cenário: Histórico - Rastreamento de Mudanças de Status
    Dado que um orçamento passou por todo o ciclo
    Quando eu consulto o histórico do orçamento
    Então deve haver registros de:
      | statusAnterior | statusNovo  | motivo                    |
      | null           | PENDENTE    | Orçamento criado          |
      | PENDENTE       | PENDENTE    | Diagnóstico recebido      |
      | PENDENTE       | APROVADO    | Cliente aprovou           |
      | APROVADO       | CONFIRMADO  | Pagamento processado      |
    E cada registro deve ter timestamp

  Cenário: Saga Completo - Happy Path do Billing
    Dado que o evento "OS_CRIADA" foi recebido
    Quando todos os passos do Saga são executados com sucesso:
      | passo | evento                  | status_resultado |
      | 1     | OS_CRIADA               | PENDENTE         |
      | 2     | DIAGNOSTICO_CONCLUIDO   | PENDENTE         |
      | 3     | Cliente aprova          | APROVADO         |
      | 4     | EXECUCAO_FINALIZADA     | APROVADO         |
      | 5     | Processar pagamento     | CONFIRMADO       |
    Então o orçamento deve estar com status "CONFIRMADO"
    E todos os eventos devem ter sido publicados
    E nenhuma compensação deve ter sido acionada
