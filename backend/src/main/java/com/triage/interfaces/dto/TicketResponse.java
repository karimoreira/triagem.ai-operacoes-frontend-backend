package com.triage.interfaces.dto;

import com.triage.domain.entity.Ticket;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversão de entidade de domínio para um mapa serializável em JSON,
 * mantendo a entidade livre de anotações de serialização.
 */
public final class TicketResponse {

    private TicketResponse() {
    }

    public static Map<String, Object> from(Ticket ticket) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ticket.id());
        map.put("customerId", ticket.customerId());
        map.put("subject", ticket.subject());
        map.put("body", ticket.body());
        map.put("status", ticket.status().name());
        map.put("priority", ticket.priority() == null ? null : ticket.priority().name());
        map.put("category", ticket.category() == null ? null : ticket.category().name());
        map.put("createdAt", ticket.createdAt().toString());
        map.put("reasoning", ticket.reasoning());
        map.put("suggestedReply", ticket.suggestedReply());
        map.put("executedActions", ticket.executedActions());
        return map;
    }
}
