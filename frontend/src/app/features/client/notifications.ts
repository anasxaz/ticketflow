import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TicketApi } from '../../core/api/ticket-api.service';
import { AppNotification } from '../../shared/models/notification.model';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe],
  template: `
    <section class="page-head row">
      <div>
        <h1>Mes <span class="gradient-text">notifications</span></h1>
        <p>Chaque étape de vos tickets, en temps réel.</p>
      </div>
      <button class="pill" (click)="load()">Actualiser</button>
    </section>
    <div class="divider-h"></div>

    @for (n of notifications(); track n.id) {
      <div class="notif">
        <span class="muted">{{ n.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
        <p>{{ n.message }}</p>
      </div>
    } @empty {
      <p>Aucune notification.</p>
    }
  `,
  styles: [
    `
      .notif {
        padding: 1rem 0;
        border-bottom: 1px solid color-mix(in srgb, var(--gray-400) 35%, transparent);
      }
      .notif p {
        margin-top: 0.25rem;
      }
    `,
  ],
})
export class Notifications implements OnInit {
  private api = inject(TicketApi);
  protected notifications = signal<AppNotification[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api
      .notifications()
      .subscribe((list) =>
        this.notifications.set([...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt))),
      );
  }
}
