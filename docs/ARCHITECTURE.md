# Arquitetura

Este documento explica as decisões de arquitetura do projeto e o funcionamento do agente de IA.

## Camadas (Clean Architecture)

A regra de ouro: **dependências apontam para dentro**. Código de uma camada externa pode depender de uma interna, nunca o inverso.

### domain/
O coração. Contém as entidades (`Ticket`, `TriageDecision`) e os objetos de valor (`Priority`, `Category`, `TicketStatus`). Não conhece banco de dados, HTTP, JSON nem IA. A entidade `Ticket` protege suas próprias invariantes: o estado só muda por métodos de negócio (`markTriaging`, `applyTriage`), nunca por setters abertos. Isso torna impossível representar um ticket em estado inválido.

### application/
A camada de casos de uso e orquestração. Define **ports** (interfaces) que descrevem o que a aplicação precisa do mundo externo:

- `port/in/TriageTicketUseCase` — o que a aplicação oferece (entrada)
- `port/out/LLMProvider` — abstração de um provedor de IA com tool calling
- `port/out/TicketRepository` — abstração de persistência
- `port/out/NotificationGateway`, `TaskManagerGateway` — integrações externas

Aqui também vive o `TriageAgent` — o orquestrador do laço do agente — e o `TriageTicketService`, que implementa o caso de uso.

### infrastructure/
As **implementações concretas** dos ports: `GroqLLMProvider`, `InMemoryTicketRepository`, `ConsoleNotificationGateway`, `InMemoryTaskManagerGateway`. Trocar qualquer uma não afeta as camadas internas.

### interfaces/
A borda do sistema: o controller HTTP, os DTOs de serialização e o **Composition Root** (`Application.java`), onde as implementações concretas são escolhidas e injetadas. É o único lugar que conhece todas as peças.

## O laço do agente

O que torna isto um *agente* e não apenas uma chamada de LLM é o laço de decisão e ação:

```
        ┌──────────────────────────────────────────────┐
        │                                              │
        ▼                                              │
  ┌───────────┐    decide     ┌─────────────┐          │
  │  Modelo   │──────────────▶│ Precisa de  │          │
  │  (LLM)    │               │ ferramenta? │          │
  └───────────┘               └─────┬───────┘          │
        ▲                           │                  │
        │                      sim  │  não             │
        │                           ▼                  │
        │                    ┌─────────────┐           │
        │   resultado da     │  Executa a  │           │
        └────────────────────│  ferramenta │           │
           ferramenta         └─────────────┘           │
                                    │                   │
                              (volta ao modelo) ────────┘
                                    │
                               não precisa
                                    ▼
                            ┌──────────────┐
                            │   Decisão    │
                            │    final     │
                            └──────────────┘
```

Em código (`TriageAgent.triage`):

1. Monta a conversa com prompt de sistema + dados do ticket.
2. Envia ao `LLMProvider` junto da lista de ferramentas disponíveis.
3. Se o modelo pede para usar ferramentas, o `ToolRegistry` as executa e o resultado volta para o modelo.
4. Repete até o modelo produzir a decisão final (ou atingir o teto de iterações, que escala por segurança).

As ferramentas (`CreateTaskTool`, `EscalateTool`) implementam a interface comum `AgentTool`. Adicionar uma capacidade nova ao agente é criar uma nova implementação — o orquestrador não muda (Open/Closed).

## Por que Java puro, sem framework

Frameworks como Spring resolvem injeção de dependência e HTTP, mas escondem o desenho. Para um projeto cujo objetivo é **demonstrar arquitetura**, fazer a injeção manual no Composition Root e usar o servidor HTTP do próprio JDK deixa as decisões explícitas e auditáveis. Em produção, trocar para Spring Boot seria direto: as camadas internas não mudariam em nada.

## Modo simulado

O `GroqLLMProvider` cai automaticamente em uma heurística local quando não há `GROQ_API_KEY`. Isso permite rodar e demonstrar o fluxo de ponta a ponta sem credenciais nem custo — útil para avaliação rápida. Com a chave, usa o modelo real com tool calling.
