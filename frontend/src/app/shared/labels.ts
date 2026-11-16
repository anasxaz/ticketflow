import { Priority, TicketCategory, TicketStatus, TriageSource } from './models/ticket.model';

export const PRIORITY_LABELS: Record<Priority, string> = {
  CRITICAL: 'Critique',
  HIGH: 'Haute',
  MEDIUM: 'Moyenne',
  LOW: 'Basse',
};

export const CATEGORY_LABELS: Record<TicketCategory, string> = {
  PANNE_TOTALE: 'Panne totale',
  SERVICE_DEGRADE: 'Service dégradé',
  FACTURATION: 'Facturation',
  DEMANDE_INFO: "Demande d'information",
};

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Ouvert',
  ASSIGNED: 'Pris en charge',
  CLOSED: 'Résolu',
  ESCALATED: 'Escaladé',
  REJECTED: 'Rejeté',
};

export const TRIAGE_LABELS: Record<TriageSource, string> = {
  FORM: 'Qualifié par formulaire',
  AI: 'Qualifié automatiquement',
  FALLBACK: 'En cours de qualification',
  HUMAN: 'Qualifié par notre équipe',
};

/** Ordre de tri : la plus urgente d'abord. */
export const PRIORITY_ORDER: Record<Priority, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
