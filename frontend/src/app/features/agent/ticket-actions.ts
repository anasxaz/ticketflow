import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TicketApi } from '../../core/api/ticket-api.service';
import { Priority, PriorityChange, Ticket } from '../../shared/models/ticket.model';
import { PRIORITY_LABELS } from '../../shared/labels';
import { PriorityChip } from '../../shared/ui/priority-chip';

/** Panneau "Gerer" d'un ticket : requalifier, resoudre, historique. */
@Component({
  selector: 'app-ticket-actions',
  imports: [DatePipe, ReactiveFormsModule, PriorityChip],
  template: `
    @if (!canAct()) {
      <p class="muted">Ce ticket est assigné à un autre agent : vous ne pouvez pas le modifier.</p>
    } @else {
      <div class="actions">
        <!-- Requalifier -->
        <form [formGroup]="form" (ngSubmit)="requalify()" class="form">
          <h4>Requalifier la priorité</h4>
          <div class="field">
            <label for="priority-{{ ticket().id }}">Nouvelle priorité</label>
            <select id="priority-{{ ticket().id }}" formControlName="priority">
              <option [ngValue]="null" disabled>Choisir</option>
              @for (p of priorities; track p) {
                @if (p !== ticket().priority) {
                  <option [ngValue]="p">{{ priorityLabels[p] }}</option>
                }
              }
            </select>
          </div>
          <div class="field">
            <label for="reason-{{ ticket().id }}">Motif (obligatoire)</label>
            <textarea
              id="reason-{{ ticket().id }}"
              formControlName="reason"
              rows="2"
              placeholder="Ex. Ligne fonctionnelle après diagnostic, simple lenteur"
            ></textarea>
          </div>
          <div>
            <button class="pill violet" type="submit" [disabled]="busy()">Requalifier</button>
          </div>
        </form>

        <!-- Resoudre -->
        @if (ticket().status === 'ASSIGNED' || ticket().status === 'ESCALATED') {
          <div>
            <h4>Résolution</h4>
            <p class="muted">Termine le processus Camunda sur « Résolu » et notifie le client.</p>
            <button class="btn" style="margin-top: 0.75rem" (click)="close()" [disabled]="busy()">
              Marquer comme résolu
            </button>
          </div>
        }
      </div>

      @if (message()) {
        <p [class.error]="isError()" class="feedback">{{ message() }}</p>
      }
    }

    <!-- Historique (audit) -->
    <h4>Historique des requalifications</h4>
    @for (h of history(); track h.id) {
      <div class="history">
        <span class="muted">{{ h.changedAt | date: 'dd/MM/yyyy HH:mm' }} · {{ h.changedBy }}</span>
        <div class="meta">
          <app-priority-chip [priority]="h.oldPriority" /> <span>→</span>
          <app-priority-chip [priority]="h.newPriority" />
          <span>{{ h.reason }}</span>
        </div>
      </div>
    } @empty {
      <p class="muted">Aucune requalification.</p>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        margin-top: 1.25rem;
        padding-top: 1.25rem;
        border-top: 1px solid color-mix(in srgb, var(--gray-400) 35%, transparent);
      }
      .actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 2rem;
        margin-bottom: 1rem;
      }
      h4 {
        font-family: var(--font-title);
        font-weight: 500;
        margin: 0 0 0.5rem;
      }
      .feedback {
        margin-bottom: 1rem;
      }
      .history {
        padding: 0.5rem 0;
      }
    `,
  ],
})
export class TicketActions implements OnInit {
  private api = inject(TicketApi);

  ticket = input.required<Ticket>();
  canAct = input.required<boolean>();
  changed = output<void>(); // previent la liste qu'il faut recharger

  protected priorities: Priority[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  protected priorityLabels = PRIORITY_LABELS;

  protected history = signal<PriorityChange[]>([]);
  protected busy = signal(false);
  protected message = signal<string | null>(null);
  protected isError = signal(false);

  protected form = new FormGroup({
    priority: new FormControl<Priority | null>(null, Validators.required),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.minLength(10)] }),
  });

  ngOnInit(): void {
    this.loadHistory();
  }

  private loadHistory(): void {
    this.api.history(this.ticket().id).subscribe((h) => this.history.set(h));
  }

  requalify(): void {
    const { priority, reason } = this.form.getRawValue();
    if (!priority) {
      return this.fail('Choisissez une priorité.');
    }
    if (reason.trim().length < 10) {
      return this.fail('Le motif doit faire au moins 10 caractères.');
    }

    this.busy.set(true);
    this.api.requalify(this.ticket().id, { priority, reason }).subscribe({
      next: () => {
        this.ok('Priorité requalifiée : le SLA a été relancé.');
        this.form.reset();
        this.loadHistory();
      },
      error: (e) => this.fail(this.explain(e.status)),
    });
  }

  close(): void {
    this.busy.set(true);
    this.api.close(this.ticket().id).subscribe({
      next: () => this.ok('Ticket résolu.'),
      error: (e) => this.fail(this.explain(e.status)),
    });
  }

  private ok(msg: string): void {
    this.busy.set(false);
    this.isError.set(false);
    this.message.set(msg);
    this.changed.emit();
  }

  private fail(msg: string): void {
    this.busy.set(false);
    this.isError.set(true);
    this.message.set(msg);
  }

  /** Traduit le code HTTP du backend en message clair. */
  private explain(status: number): string {
    switch (status) {
      case 400:
        return 'Demande refusée : motif trop court ou priorité identique.';
      case 403:
        return "Seul l'agent assigné peut modifier ce ticket.";
      default:
        return `Erreur ${status}.`;
    }
  }
}
