import { CreateTicketInput, Ticket } from "../types/ticket";

const BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";

function normalize(raw: Partial<Ticket>): Ticket {
  return {
    id: raw.id ?? "",
    customerId: raw.customerId ?? "",
    subject: raw.subject ?? "",
    body: raw.body ?? "",
    status: raw.status ?? "RECEIVED",
    priority: raw.priority ?? null,
    category: raw.category ?? null,
    createdAt: raw.createdAt ?? new Date().toISOString(),
    reasoning: raw.reasoning ?? null,
    suggestedReply: raw.suggestedReply ?? null,
    executedActions: raw.executedActions ?? [],
  };
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erro ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function fetchTickets(): Promise<Ticket[]> {
  const response = await fetch(`${BASE_URL}/tickets`);
  const raw = await handleResponse<Partial<Ticket>[]>(response);
  return raw.map(normalize);
}

export async function createTicket(
  input: CreateTicketInput
): Promise<Ticket> {
  const response = await fetch(`${BASE_URL}/tickets`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const raw = await handleResponse<Partial<Ticket>>(response);
  return normalize(raw);
}
