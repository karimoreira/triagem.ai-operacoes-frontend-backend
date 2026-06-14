package com.triage.interfaces.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.triage.application.port.in.TriageTicketUseCase;
import com.triage.application.port.in.TriageTicketUseCase.Command;
import com.triage.application.port.out.TicketRepository;
import com.triage.domain.entity.Ticket;
import com.triage.infrastructure.config.Json;
import com.triage.interfaces.dto.TicketResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter de entrada HTTP. Traduz requisições HTTP em chamadas ao caso de
 * uso e serializa o resultado. Depende dos ports (use case e repositório),
 * não das implementações — Dependency Inversion na borda do sistema.
 *
 * Rotas:
 *   POST /api/tickets   -> cria e tria um ticket
 *   GET  /api/tickets   -> lista todos os tickets
 */
public final class TicketController implements HttpHandler {

    private final TriageTicketUseCase triageTicketUseCase;
    private final TicketRepository ticketRepository;

    public TicketController(TriageTicketUseCase triageTicketUseCase, TicketRepository ticketRepository) {
        this.triageTicketUseCase = triageTicketUseCase;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();

        try {
            if ("OPTIONS".equalsIgnoreCase(method)) {
                send(exchange, 204, "");
                return;
            }
            if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
                return;
            }
            if ("GET".equalsIgnoreCase(method)) {
                handleList(exchange);
                return;
            }
            send(exchange, 405, Json.write(Map.of("error", "Método não suportado")));
        } catch (IllegalArgumentException ex) {
            send(exchange, 400, Json.write(Map.of("error", ex.getMessage())));
        } catch (Exception ex) {
            send(exchange, 500, Json.write(Map.of("error", "Erro interno: " + ex.getMessage())));
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> input = Json.toMap(body);

        Command command = new Command(
            stringOf(input.get("customerId")),
            stringOf(input.get("subject")),
            stringOf(input.get("body")));

        Ticket ticket = triageTicketUseCase.handle(command);
        send(exchange, 201, Json.write(TicketResponse.from(ticket)));
    }

    private void handleList(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> payload = ticketRepository.findAll().stream()
            .map(TicketResponse::from)
            .collect(Collectors.toList());
        send(exchange, 200, Json.write(payload));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
