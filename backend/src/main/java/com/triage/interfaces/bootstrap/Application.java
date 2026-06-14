package com.triage.interfaces.bootstrap;

import com.sun.net.httpserver.HttpServer;
import com.triage.application.agent.AgentTool;
import com.triage.application.agent.ToolRegistry;
import com.triage.application.agent.TriageAgent;
import com.triage.application.agent.tool.CreateTaskTool;
import com.triage.application.agent.tool.EscalateTool;
import com.triage.application.port.in.TriageTicketUseCase;
import com.triage.application.port.out.LLMProvider;
import com.triage.application.port.out.NotificationGateway;
import com.triage.application.port.out.TaskManagerGateway;
import com.triage.application.port.out.TicketRepository;
import com.triage.application.usecase.TriageTicketService;
import com.triage.infrastructure.integration.ConsoleNotificationGateway;
import com.triage.infrastructure.integration.InMemoryTaskManagerGateway;
import com.triage.infrastructure.llm.GroqLLMProvider;
import com.triage.infrastructure.persistence.InMemoryTicketRepository;
import com.triage.interfaces.http.TicketController;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Composition Root: ponto único onde as implementações concretas são
 * escolhidas e injetadas nas abstrações. Todo o resto do sistema depende
 * apenas de interfaces. Para trocar Groq por OpenAI, ou o repositório em
 * memória por um banco, basta alterar esta classe.
 */
public final class Application {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        // --- Adapters de saída (infraestrutura) ---
        TicketRepository ticketRepository = new InMemoryTicketRepository();
        LLMProvider llmProvider = new GroqLLMProvider();
        NotificationGateway notificationGateway = new ConsoleNotificationGateway();
        TaskManagerGateway taskManagerGateway = new InMemoryTaskManagerGateway();

        // --- Ferramentas do agente (Open/Closed: lista extensível) ---
        List<AgentTool> tools = List.of(
            new CreateTaskTool(taskManagerGateway),
            new EscalateTool(notificationGateway));
        ToolRegistry toolRegistry = new ToolRegistry(tools);

        // --- Agente e caso de uso (aplicação) ---
        TriageAgent triageAgent = new TriageAgent(llmProvider, toolRegistry);
        TriageTicketUseCase triageTicketUseCase =
            new TriageTicketService(ticketRepository, triageAgent);

        // --- Adapter de entrada (HTTP) ---
        TicketController controller = new TicketController(triageTicketUseCase, ticketRepository);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/tickets", controller);
        // Executor com threads non-daemon mantém a JVM viva enquanto o servidor roda.
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        boolean usingRealLlm = System.getenv("GROQ_API_KEY") != null
            && !System.getenv("GROQ_API_KEY").isBlank();
        System.out.println("Servidor de triagem em http://localhost:" + PORT + "/api/tickets");
        System.out.println("Modo LLM: " + (usingRealLlm ? "Groq (real)" : "simulado (defina GROQ_API_KEY para usar IA real)"));
    }
}
