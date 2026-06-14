import { useMemo, useState } from "react";
import { ComposeTicket } from "./components/ComposeTicket";
import { TicketCard } from "./components/TicketCard";
import { useTickets } from "./hooks/useTickets";
import { Ticket } from "./types/ticket";

export default function App() {
  const { tickets, loading, submitting, error, submit, reload } = useTickets();
  const [showForm, setShowForm] = useState(false);
  const [showResolved, setShowResolved] = useState(false);
  const [attended, setAttended] = useState<Set<string>>(new Set());

  const { pending, resolved } = useMemo(() => {
    const pending: Ticket[] = [];
    const resolved: Ticket[] = [];
    for (const t of tickets) {
      if (t.status === "AUTO_RESOLVED") resolved.push(t);
      else pending.push(t);
    }
    return { pending, resolved };
  }, [tickets]);

  const stats = useMemo(() => {
    const total = tickets.length;
    const escalated = tickets.filter((t) => t.status === "ESCALATED").length;
    const critical = tickets.filter((t) => t.priority === "CRITICAL").length;
    const triaged = tickets.filter(
      (t) => t.status !== "RECEIVED" && t.status !== "TRIAGING"
    ).length;
    return { total, escalated, critical, triaged, resolved: resolved.length };
  }, [tickets, resolved.length]);

  function handleAttend(ticket: Ticket) {
    setAttended((prev) => new Set(prev).add(ticket.id));
  }

  return (
    <div className="app">
      <header className="masthead">
        <div>
          <h1>
            Triagem<span className="pulse">.ai</span>
          </h1>
          <div className="tagline">
            agente autônomo · classificação · resolução
          </div>
        </div>
        <div className="live-badge">
          <span className="dot" />
          ao vivo
        </div>
      </header>

      <section className="stats">
        <div className="stat">
          <div className="num">{stats.total}</div>
          <div className="lbl">Chamados</div>
        </div>
        <div className="stat">
          <div className="num">{stats.triaged}</div>
          <div className="lbl">Triados pela IA</div>
        </div>
        <div className="stat">
          <div className="num">{stats.resolved}</div>
          <div className="lbl">Resolvidos pela IA</div>
        </div>
        <div className="stat critical">
          <div className="num">{stats.escalated}</div>
          <div className="lbl">Escalados</div>
        </div>
      </section>

      <div className="toolbar">
        <button className="btn-new" onClick={() => setShowForm(!showForm)}>
          {showForm ? "× fechar" : "+ novo chamado"}
        </button>
        <span className="toolbar-hint">
          {showForm ? "Preencha os dados para testar o agente" : "Clique para testar o agente com um chamado"}
        </span>
      </div>

      {showForm && (
        <div className="form-overlay">
          <ComposeTicket onSubmit={submit} submitting={submitting} />
        </div>
      )}

      {error && <div className="error-box">{error}</div>}

      <section className="feed">
        <div className="feed-head">
          <h2>Fila de triagem <span className="pending-count">{pending.length}</span></h2>
          <button className="refresh" onClick={() => void reload()}>
            ↻ atualizar
          </button>
        </div>

        {loading ? (
          <div className="ticket-list">
            <div className="skeleton" />
            <div className="skeleton" />
            <div className="skeleton" />
          </div>
        ) : pending.length === 0 ? (
          <div className="empty">
            Nenhum chamado pendente. Clique em "+ novo chamado" e veja o agente
            classificar e resolver em tempo real.
          </div>
        ) : (
          <div className="ticket-list">
            {pending.map((ticket) => (
              <TicketCard
                key={ticket.id}
                ticket={ticket}
                attended={attended.has(ticket.id)}
                onAttend={() => handleAttend(ticket)}
              />
            ))}
          </div>
        )}

        {resolved.length > 0 && (
          <>
            <button className="toggle-resolved" onClick={() => setShowResolved(!showResolved)}>
              {showResolved ? "▲ esconder resolvidos" : "▼ resolvidos pela IA (" + resolved.length + ")"}
            </button>
            {showResolved && (
              <div className="ticket-list resolved-list">
                {resolved.map((ticket) => (
                  <TicketCard
                    key={ticket.id}
                    ticket={ticket}
                    attended={false}
                    onAttend={() => {}}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
