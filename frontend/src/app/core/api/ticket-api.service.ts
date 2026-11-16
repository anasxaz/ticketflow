import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CreateTicketRequest, PriorityChange, RequalifyRequest, Ticket, UserProfile } from '../../shared/models/ticket.model';
import { AppNotification } from '../../shared/models/notification.model';


/** Tous les appels HTTP au meme endroit (le token est ajoute par l'intercepteur). */
@Injectable({ providedIn: 'root' })
export class TicketApi {
  private http = inject(HttpClient);

  // ----- Client -----

  create(body: CreateTicketRequest) {
    return this.http.post<Ticket>('/api/tickets', body);
  }

  mine() {
    return this.http.get<Ticket[]>('/api/tickets/mine');
  }

  notifications() {
    return this.http.get<AppNotification[]>('/api/notifications');
  }

  // ----- Agent -----
  all() {
    return this.http.get<Ticket[]>('/api/tickets');
  }

  requalify(id: number, body: RequalifyRequest) {
    return this.http.patch<Ticket>(`/api/tickets/${id}/priority`, body);
  }

  close(id: number) {
    return this.http.patch<Ticket>(`/api/tickets/${id}/status`, { status: 'CLOSED' });
  }

  history(id: number) {
    return this.http.get<PriorityChange[]>(`/api/tickets/${id}/priority-history`);
  }

  me(email: string) {
    return this.http.get<UserProfile>('/api/users/by-email', { params: { email } });
  }
}
