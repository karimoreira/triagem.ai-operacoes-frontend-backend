package com.triage.application.port.out;

import com.triage.domain.entity.Ticket;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    void save(Ticket ticket);
    Optional<Ticket> findById(String id);
    List<Ticket> findAll();
}
