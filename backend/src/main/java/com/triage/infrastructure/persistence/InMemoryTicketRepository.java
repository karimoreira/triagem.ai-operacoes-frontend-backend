package com.triage.infrastructure.persistence;

import com.triage.application.port.out.TicketRepository;
import com.triage.domain.entity.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementação em memória do repositório, thread-safe. Adequada para
 * demonstração e testes. Trocar por JPA/JDBC não afeta a regra de negócio,
 * pois ambos implementam o mesmo port TicketRepository.
 */
public final class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> store = new ConcurrentHashMap<>();

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
