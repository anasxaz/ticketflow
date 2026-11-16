import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TicketApi } from '../../core/api/ticket-api.service';
import { Ticket } from '../../shared/models/ticket.model';
import { CATEGORY_LABELS, STATUS_LABELS, TRIAGE_LABELS } from '../../shared/labels';
import { PriorityChip } from '../../shared/ui/priority-chip';

@Component({
  selector: 'app-my-tickets',
  imports: [DatePipe, PriorityChip],
  template: `
    <section class="page-head row">
      <div>
        <h1>Mes <span class="gradient-text">tickets</span></h1>
        <p>Suivez l'avancement de vos demandes.</p>
      </div>
      <button class="pill" (click)="load()">Actualiser</button>
    </section>
    <div class="divider-h"></div>

    @if (loading()) {
      <p class="muted">Chargement…</p>
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      @for (t of tickets(); track t.id) {
        <article class="card">
          <div class="card-top">
            <span class="muted">#{{ t.id }} · {{ t.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
            <span class="status">{{ statusLabels[t.status] }}</span>
          </div>
          <h3>{{ t.title }}</h3>
          <div class="meta">
            <app-priority-chip [priority]="t.priority" />
            @if (t.category) {
              <span>{{ categoryLabels[t.category] }}</span>
            }
            <span class="muted">{{ triageLabels[t.triageSource] }}</span>
          </div>
          @if (t.description) {
            <p class="desc">{{ t.description }}</p>
          }
        </article>
      } @empty {
        <p>Vous n'avez encore aucun ticket.</p>
      }
    }
  `,
  styles: [
    `
      .status {
        font-size: 0.8125rem;
        font-weight: 600;
        color: var(--gray-700);
      }
      .desc {
        margin-top: 0.75rem;
      }
    `,
  ],
})
export class MyTickets implements OnInit {
  private api = inject(TicketApi);

  protected tickets = signal<Ticket[]>([]);
  protected loading = signal(true);
  protected error = signal<string | null>(null);

  protected statusLabels = STATUS_LABELS;
  protected categoryLabels = CATEGORY_LABELS;
  protected triageLabels = TRIAGE_LABELS;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.mine().subscribe({
      next: (list) => {
        this.tickets.set(list);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(`Erreur ${e.status}`);
        this.loading.set(false);
      },
    });
  }
}
