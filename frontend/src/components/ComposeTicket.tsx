import { FormEvent, useState } from "react";
import { CreateTicketInput } from "../types/ticket";

interface ComposeTicketProps {
  onSubmit: (input: CreateTicketInput) => Promise<void>;
  submitting: boolean;
}

/**
 * Formulário de abertura de ticket. Mantém apenas estado local de input;
 * delega a submissão ao componente pai via callback (lifting state up).
 */
export function ComposeTicket({ onSubmit, submitting }: ComposeTicketProps) {
  const [customerId, setCustomerId] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");

  const canSubmit =
    customerId.trim() && subject.trim() && body.trim() && !submitting;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!canSubmit) return;
    try {
      await onSubmit({
        customerId: customerId.trim(),
        subject: subject.trim(),
        body: body.trim(),
      });
      setSubject("");
      setBody("");
    } catch {
      /* erro é exibido pelo componente pai */
    }
  }

  return (
    <form className="panel" onSubmit={handleSubmit}>
      <div className="panel-title">Abrir chamado</div>

      <div className="field">
        <label htmlFor="customerId">Cliente</label>
        <input
          id="customerId"
          value={customerId}
          onChange={(e) => setCustomerId(e.target.value)}
          placeholder="cust-001"
        />
      </div>

      <div className="field">
        <label htmlFor="subject">Assunto</label>
        <input
          id="subject"
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
          placeholder="Não consigo acessar minha conta"
        />
      </div>

      <div className="field">
        <label htmlFor="body">Descrição</label>
        <textarea
          id="body"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Descreva o problema com o máximo de detalhes…"
        />
      </div>

      <button className="btn" type="submit" disabled={!canSubmit}>
        {submitting ? "Triando com IA…" : "Enviar para triagem"}
      </button>

      <p className="hint">
        O agente de IA classifica prioridade e categoria, decide se escala
        para um humano e pode abrir tarefas automaticamente.
      </p>
    </form>
  );
}
