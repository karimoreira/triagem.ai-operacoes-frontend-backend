# Triagem.ai - Agente de IA para triagem de tickets de suporte

Sistema full-stack que demonstra **automação orientada por agente de IA** aplicada a um problema real: triar tickets de suporte. Um agente autônomo analisa cada chamado, classifica prioridade e categoria, decide se escala para um humano e pode **executar ações** (abrir tarefas, notificar equipes) por conta própria via *tool calling*.

O foco do projeto é mostrar, com código de produção, três competências que o mercado busca em conjunto: **integração com IA**, **agentes que decidem e agem**, e **arquitetura de software limpa e sustentável**.

---

## Por que este projeto

A maioria dos exemplos de "IA" para portfólio é só uma chamada de API que devolve texto. Aqui a diferença está no **agente**: o modelo não apenas responde — ele recebe um conjunto de ferramentas, decide quais usar, executa, observa o resultado e repete até concluir a triagem. Esse laço (decidir → agir → observar) é o que separa "usei um LLM" de "construí um agente".

Tudo isso sobre uma base de **Clean Architecture**, com inversão de dependência real: a regra de negócio não conhece o provedor de IA, o banco, nem o protocolo HTTP.

---

## Stack

| Camada | Tecnologia | Por quê |
|--------|-----------|---------|
| Backend | **Java 21** puro (sem frameworks) | Mostra domínio de Clean Architecture sem depender de "mágica" de framework |
| IA | **Groq** (Llama 3.3) via *tool calling* | Tier gratuito, rápido; atrás de uma interface trocável |
| Frontend | **React 18 + TypeScript + Vite** | Stack moderna de mercado, tipagem estrita |
| HTTP | `com.sun.net.httpserver` (JDK) | Servidor embutido, zero dependências |

> O backend roda **sem nenhuma dependência externa** — compila com `javac` puro. Isso é proposital: deixa a arquitetura em evidência, sem ruído de configuração.

---

## Arquitetura

O projeto segue **Clean Architecture / Ports & Adapters**. A regra de dependência aponta sempre para dentro: a infraestrutura depende da aplicação, que depende do domínio — nunca o contrário.

```
┌─────────────────────────────────────────────────────────┐
│  interfaces/         HTTP, DTOs, Composition Root         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  infrastructure/   Groq, repositório, integrações   │  │
│  │  ┌─────────────────────────────────────────────┐   │  │
│  │  │  application/   casos de uso, agente, ports  │   │  │
│  │  │  ┌───────────────────────────────────────┐  │   │  │
│  │  │  │  domain/   Ticket, TriageDecision,     │  │   │  │
│  │  │  │            Priority — regras puras     │  │   │  │
│  │  │  └───────────────────────────────────────┘  │   │  │
│  │  └─────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

Veja [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) para o detalhamento e o diagrama do laço do agente.

### Princípios SOLID em prática

- **S** — cada classe tem uma responsabilidade: `TriageAgent` orquestra, `Ticket` protege invariantes, `GroqLLMProvider` só fala com a API.
- **O** — adicionar uma ferramenta ao agente é criar uma nova classe `AgentTool`; o orquestrador não muda.
- **L** — qualquer `LLMProvider` é substituível sem quebrar o agente.
- **I** — ports pequenos e focados (`NotificationGateway`, `TaskManagerGateway`).
- **D** — a aplicação depende de interfaces; as implementações são injetadas no *Composition Root* (`Application.java`).

---

## Como rodar

### Backend

```bash
cd backend
javac -d out $(find src/main -name "*.java")
java -cp out com.triage.interfaces.bootstrap.Application
# Servidor em http://localhost:8080/api/tickets
```

Por padrão roda em **modo simulado** (sem custo, sem chave). Para usar IA real, exporte uma chave gratuita da Groq:

```bash
export GROQ_API_KEY="sua-chave"   # https://console.groq.com
```

### Testes

```bash
cd backend
javac -cp out -d out src/test/java/com/triage/application/TriageTicketServiceTest.java
java -cp out com.triage.application.TriageTicketServiceTest
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# App em http://localhost:5173
```

---

## API

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/tickets` | Cria e tria um ticket. Corpo: `{ customerId, subject, body }` |
| `GET`  | `/api/tickets` | Lista todos os tickets triados |

Exemplo:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-42","subject":"Sistema parou","body":"erro 500 urgente"}'
```

Resposta:

```json
{
  "id": "2faeb693-...",
  "status": "ESCALATED",
  "priority": "CRITICAL",
  "category": "TECHNICAL"
}
```

---

## O que este projeto demonstra

- Integração com LLM via *tool calling* — o agente decide e executa ações
- Clean Architecture com fronteiras reais entre camadas
- SOLID aplicado, não recitado
- Testabilidade: a regra de negócio é testada sem rede nem banco
- Frontend tipado e desacoplado da API por uma camada própria

## Próximos passos possíveis

- Persistência real (PostgreSQL) implementando o mesmo `TicketRepository`
- Fila assíncrona para triagem em lote
- Adapters reais de Slack e Jira no lugar dos simulados
- Streaming da decisão do agente em tempo real para o frontend
