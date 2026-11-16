export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus = 'OPEN' | 'ASSIGNED' | 'CLOSED' | 'ESCALATED' | 'REJECTED';
export type TicketCategory = 'PANNE_TOTALE' | 'SERVICE_DEGRADE' | 'FACTURATION' | 'DEMANDE_INFO';
export type TriageSource = 'FORM' | 'AI' | 'FALLBACK' | 'HUMAN';

/** Ce que renvoie GET /api/tickets/{id} (miroir de l'entite Ticket Java). */
export interface Ticket {
  id: number;
  title: string;
  description: string | null;
  status: TicketStatus;
  priority: Priority;
  category: TicketCategory | null;
  blocked: boolean | null;
  clientId: number;
  agentId: number | null;
  triageSource: TriageSource;
  aiConfidence: number | null;
  needsReview: boolean;
  createdAt: string;
}

/** Corps de POST /api/tickets : texte libre (le LLM trie) ou formulaire. */
export interface CreateTicketRequest {
  title: string;
  description?: string;
  category?: TicketCategory;
  blocked?: boolean;
}

/** Corps de PATCH /api/tickets/{id}/priority. */
export interface RequalifyRequest {
  priority: Priority;
  reason: string;
}
