import { useState } from "react";
import { Ticket } from "../types/ticket";

interface TicketCardProps {
  ticket: Ticket;
  attended: boolean;
  onAttend: () => void;
  onClose: () => void;
}

const STATUS_LABEL: Record<Ticket["status"], string> = {
  RECEIVED: "Recebido",
  TRIAGING: "Triando",
  TRIAGED: "Triado",
  ESCALATED: "Escalado",
  AUTO_RESOLVED: "Resolvido",
  FAILED: "Falhou",
};

const CATEGORY_LABEL: Record<string, string> = {
  BILLING: "Cobrança",
  TECHNICAL: "Técnico",
  ACCOUNT: "Conta",
  FEEDBACK: "Feedback",
  OTHER: "Outro",
};

export function TicketCard({ ticket, attended, onAttend, onClose }: TicketCardProps) {
  const [expanded, setExpanded] = useState(true);
  const sevClass = ticket.priority ? `sev-${ticket.priority}` : "";
  const hasDetails = !!ticket.reasoning || !!ticket.suggestedReply || (ticket.executedActions?.length ?? 0) > 0;

  return (
    <article className={`ticket ${sevClass} ${attended ? "attended" : ""}`}>
      <div className="ticket-top">
        <div>
          <div className="ticket-subject">{ticket.subject}</div>
          <div className="ticket-cust">{ticket.customerId}</div>
        </div>
      </div>

      <p className="ticket-body">{ticket.body}</p>

      <div className="ticket-meta">
        {ticket.priority && (
          <span className={`tag prio ${ticket.priority}`}>
            {ticket.priority}
          </span>
        )}
        {ticket.category && (
          <span className="tag">{CATEGORY_LABEL[ticket.category]}</span>
        )}
        <span className={`tag status-${ticket.status}`}>
          {STATUS_LABEL[ticket.status]}
        </span>
        {attended && <span className="tag attended-tag">Humano atendendo</span>}
      </div>

      {ticket.status === "ESCALATED" && !attended && (
        <button className="btn-attend" onClick={onAttend}>
          Atender chamado
        </button>
      )}

      {attended && (
        <div className="auto-reply">
          <div className="auto-reply-label">Resposta automática enviada ao cliente</div>
          <div className="auto-reply-text">Recebemos seu chamado e já estamos analisando.</div>
          <button className="btn-close" onClick={onClose}>
            Fechar chamado
          </button>
        </div>
      )}

      {hasDetails && (
        <button className="toggle-details" onClick={() => setExpanded(!expanded)}>
          {expanded ? "▲ detalhes da IA" : "▼ decisão da IA"}
        </button>
      )}

      {expanded && hasDetails && (
        <div className="agent-details">
          {ticket.reasoning && (
            <div className="detail-section">
              <div className="detail-label">Raciocínio do agente</div>
              <div className="detail-text">{ticket.reasoning}</div>
            </div>
          )}

          {ticket.executedActions && ticket.executedActions.length > 0 && (
            <div className="detail-section">
              <div className="detail-label">Ações executadas</div>
              {ticket.executedActions.map((action, i) => (
                <div key={i} className="detail-action">{action}</div>
              ))}
            </div>
          )}
        </div>
      )}
    </article>
  );
}
