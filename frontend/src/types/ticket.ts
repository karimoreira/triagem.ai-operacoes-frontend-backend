export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type Category =
  | "BILLING"
  | "TECHNICAL"
  | "ACCOUNT"
  | "FEEDBACK"
  | "OTHER";

export type TicketStatus =
  | "RECEIVED"
  | "TRIAGING"
  | "TRIAGED"
  | "ESCALATED"
  | "AUTO_RESOLVED"
  | "FAILED";

export interface Ticket {
  id: string;
  customerId: string;
  subject: string;
  body: string;
  status: TicketStatus;
  priority: Priority | null;
  category: Category | null;
  createdAt: string;
  reasoning: string | null;
  suggestedReply: string | null;
  executedActions: string[];
}

export interface CreateTicketInput {
  customerId: string;
  subject: string;
  body: string;
}
