import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CreateTicketRequest, Ticket } from '../../shared/models/ticket.model';
import { AppNotification } from '../../shared/models/notification.model';

/** Tous les appels HTTP au meme endroit (le token est ajoute par l'intercepteur). */
@Injectable({ providedIn: 'root' })
export class TicketApi {
  private http = inject(HttpClient);

  create(body: CreateTicketRequest) {
    return this.http.post<Ticket>('/api/tickets', body);
  }

  mine() {
    return this.http.get<Ticket[]>('/api/tickets/mine');
  }

  notifications() {
    return this.http.get<AppNotification[]>('/api/notifications');
  }
}
