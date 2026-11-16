import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TicketApi } from '../../core/api/ticket-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Ticket } from '../../shared/models/ticket.model';
import { CATEGORY_LABELS, PRIORITY_ORDER, STATUS_LABELS, TRIAGE_LABELS } from '../../shared/labels';
import { PriorityChip } from '../../shared/ui/priority-chip';
import { TicketActions } from './ticket-actions';

type Filter = 'mine' | 'active' | 'all';
const DONE = ['CLOSED', 'REJECTED'];

@Component({
  selector: 'app-agent-tickets',
  imports: [DatePipe, PriorityChip, TicketActions],
  template: `
    <section class="page-head row">
      <div>
        <h1>Espace <span class="gradient-text">agent</span></h1>
        <p>Traitez les tickets, de la plus haute priorité à la plus basse.</p>
      </div>
      <button class="pill" (click)="load()">Actualiser</button>
    </section>

    <nav class="filters">
      <button class="pill violet" [class.active]="filter() === 'mine'" (click)="filter.set('mine')">
        À traiter ({{ count('mine') }})
      </button>
      <button
        class="pill purple"
        [class.active]="filter() === 'active'"
        (click)="filter.set('active')"
      >
        En cours ({{ count('active') }})
      </button>
      <button class="pill" [class.active]="filter() === 'all'" (click)="filter.set('all')">
        Tous ({{ count('all') }})
      </button>
    </nav>
    <div class="divider-h"></div>

    @for (t of visible(); track t.id) {
      <article class="card">
        <div class="card-top">
          <span class="muted">
            #{{ t.id }} · {{ t.createdAt | date: 'dd/MM/yyyy HH:mm' }} · client {{ t.clientId }}
            @if (t.agentId) {
              · agent {{ t.agentId }}
            }
          </span>
          <span class="status">{{ statusLabels[t.status] }}</span>
        </div>
        <h3>{{ t.title }}</h3>
        <div class="meta">
          <app-priority-chip [priority]="t.priority" />
          @if (t.category) {
            <span>{{ categoryLabels[t.category] }}</span>
          }
          <span class="muted">{{ triageLabels[t.triageSource] }}</span>
          @if (t.aiConfidence !== null) {
            <span class="muted">confiance {{ t.aiConfidence }}</span>
          }
        </div>
        @if (t.description) {
          <p class="desc">{{ t.description }}</p>
        }

        <p style="margin-top: 1rem">
          <button class="pill purple" (click)="toggle(t.id)">
            {{ selectedId() === t.id ? 'Fermer' : 'Gérer' }}
          </button>
        </p>

        @if (selectedId() === t.id) {
          <app-ticket-actions [ticket]="t" [canAct]="t.agentId === meId()" (changed)="load()" />
        }
      </article>
    } @empty {
      <p>Aucun ticket dans cette vue.</p>
    }
  `,
  styles: [
    `
      .filters {
        display: flex;
        gap: 0.5rem;
        margin-top: 1.5rem;
      }
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
export class AgentTickets implements OnInit {
  private api = inject(TicketApi);
  private auth = inject(AuthService);

  protected tickets = signal<Ticket[]>([]);
  protected meId = signal<number | null>(null);
  protected filter = signal<Filter>('mine');
  protected selectedId = signal<number | null>(null);

  protected statusLabels = STATUS_LABELS;
  protected categoryLabels = CATEGORY_LABELS;
  protected triageLabels = TRIAGE_LABELS;

  /** Recalcule automatiquement quand tickets, filtre ou meId changent. */
  protected visible = computed(() =>
    this.apply(this.filter()).sort(
      (a, b) =>
        PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority] ||
        b.createdAt.localeCompare(a.createdAt),
    ),
  );

  ngOnInit(): void {
    this.api.me(this.auth.email).subscribe((me) => this.meId.set(me.id)); // mon id dans user-service
    this.load();
  }

  load(): void {
    this.api.all().subscribe((list) => this.tickets.set(list));
  }

  toggle(id: number): void {
    this.selectedId.set(this.selectedId() === id ? null : id);
  }

  count(f: Filter): number {
    return this.apply(f).length;
  }

  private apply(f: Filter): Ticket[] {
    const list = this.tickets();
    switch (f) {
      case 'mine':
        return list.filter((t) => t.agentId === this.meId() && !DONE.includes(t.status));
      case 'active':
        return list.filter((t) => !DONE.includes(t.status));
      default:
        return [...list];
    }
  }
}
