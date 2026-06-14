package com.triage.application;

import com.triage.application.agent.ToolRegistry;
import com.triage.application.agent.TriageAgent;
import com.triage.application.port.in.TriageTicketUseCase.Command;
import com.triage.application.port.out.LLMProvider;
import com.triage.application.port.out.TicketRepository;
import com.triage.application.usecase.TriageTicketService;
import com.triage.domain.entity.Ticket;
import com.triage.domain.valueobject.TicketStatus;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Teste do caso de uso usando dublês (fakes) das dependências. Demonstra
 * que a regra de negócio é testável isoladamente, sem rede nem banco —
 * benefício direto da inversão de dependência. Java puro, sem JUnit, para
 * rodar com javac/java sem gerenciador de dependências.
 */
public final class TriageTicketServiceTest {

    public static void main(String[] args) {
        int failures = 0;
        failures += run("triagem persiste ticket e aplica decisão", TriageTicketServiceTest::shouldTriageAndPersist);
        failures += run("ticket sem assunto é rejeitado", TriageTicketServiceTest::shouldRejectInvalidTicket);

        if (failures == 0) {
            System.out.println("\nTodos os testes passaram.");
        } else {
            System.out.println("\n" + failures + " teste(s) falharam.");
            System.exit(1);
        }
    }

    private static void shouldTriageAndPersist() {
        FakeTicketRepository repo = new FakeTicketRepository();
        LLMProvider stubLlm = (messages, tools) ->
            new LLMProvider.LLMResponse(
                "{\"priority\":\"HIGH\",\"category\":\"TECHNICAL\",\"shouldEscalate\":false,"
                    + "\"suggestedReply\":\"ok\",\"reasoning\":\"teste\"}",
                List.of());
        TriageAgent agent = new TriageAgent(stubLlm, new ToolRegistry(List.of()));
        TriageTicketService service = new TriageTicketService(repo, agent);

        Ticket result = service.handle(new Command("cust-1", "App caiu", "Erro 500 ao logar"));

        // triagem é assíncrona — espera até concluir (timeout 5s)
        TicketStatus finalStatus = waitForTriage(repo, result.id(), 5000);
        assertEquals(TicketStatus.AUTO_RESOLVED, finalStatus, "status final deve ser AUTO_RESOLVED");
        assertTrue(repo.findById(result.id()).isPresent(), "ticket deve estar persistido");
    }

    private static TicketStatus waitForTriage(FakeTicketRepository repo, String id, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        TicketStatus status;
        do {
            status = repo.findById(id).map(Ticket::status).orElse(null);
            if (status == TicketStatus.TRIAGED || status == TicketStatus.ESCALATED
                || status == TicketStatus.AUTO_RESOLVED || status == TicketStatus.FAILED) {
                return status;
            }
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        } while (System.currentTimeMillis() < deadline);
        return status;
    }

    private static void shouldRejectInvalidTicket() {
        FakeTicketRepository repo = new FakeTicketRepository();
        LLMProvider stubLlm = (messages, tools) -> new LLMProvider.LLMResponse("{}", List.of());
        TriageAgent agent = new TriageAgent(stubLlm, new ToolRegistry(List.of()));
        TriageTicketService service = new TriageTicketService(repo, agent);

        boolean threw = false;
        try {
            service.handle(new Command("cust-1", "", "corpo"));
        } catch (RuntimeException ex) {
            threw = true;
        }
        assertTrue(threw, "deve lançar exceção para assunto vazio");
    }

    // ---------- mini framework de teste ----------

    private interface TestCase {
        void run();
    }

    private static int run(String name, TestCase test) {
        try {
            test.run();
            System.out.println("[PASS] " + name);
            return 0;
        } catch (AssertionError | RuntimeException ex) {
            System.out.println("[FAIL] " + name + " -> " + ex.getMessage());
            return 1;
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(msg + " (esperado=" + expected + ", obtido=" + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    // ---------- fake ----------

    private static final class FakeTicketRepository implements TicketRepository {
        private final Map<String, Ticket> store = new HashMap<>();

        @Override
        public void save(Ticket ticket) {
            store.put(ticket.id(), ticket);
        }

        @Override
        public Optional<Ticket> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Ticket> findAll() {
            return new ArrayList<>(store.values());
        }
    }
}
