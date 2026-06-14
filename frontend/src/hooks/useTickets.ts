import { useCallback, useEffect, useRef, useState } from "react";
import { createTicket, fetchTickets } from "../api/ticketApi";
import { CreateTicketInput, Ticket } from "../types/ticket";

const POLL_INTERVAL = 3000;

interface UseTicketsResult {
  tickets: Ticket[];
  loading: boolean;
  submitting: boolean;
  error: string | null;
  submit: (input: CreateTicketInput) => Promise<void>;
  reload: () => Promise<void>;
}

export function useTickets(): UseTicketsResult {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval>>();

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchTickets();
      const sorted = [...data].sort((a, b) =>
        b.createdAt.localeCompare(a.createdAt)
      );
      setTickets(sorted);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar");
    } finally {
      setLoading(false);
    }
  }, []);

  const submit = useCallback(
    async (input: CreateTicketInput) => {
      setSubmitting(true);
      setError(null);
      try {
        const created = await createTicket(input);
        setTickets((prev) => [created, ...prev]);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Falha ao enviar");
        throw err;
      } finally {
        setSubmitting(false);
      }
    },
    []
  );

  useEffect(() => {
    void reload();
    pollRef.current = setInterval(() => void reload(), POLL_INTERVAL);
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, [reload]);

  return { tickets, loading, submitting, error, submit, reload };
}
