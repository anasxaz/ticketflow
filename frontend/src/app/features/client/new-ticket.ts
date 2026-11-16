import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TicketApi } from '../../core/api/ticket-api.service';
import { CreateTicketRequest, Ticket, TicketCategory } from '../../shared/models/ticket.model';
import { CATEGORY_LABELS, TRIAGE_LABELS } from '../../shared/labels';
import { PriorityChip } from '../../shared/ui/priority-chip';

@Component({
  selector: 'app-new-ticket',
  imports: [ReactiveFormsModule, RouterLink, PriorityChip],
  template: `
    <section class="page-head">
      <h1>Nouveau <span class="gradient-text">ticket</span></h1>
      <p>Décrivez votre problème avec vos mots : nous le qualifions automatiquement.</p>
    </section>
    <div class="divider-h"></div>

    <form [formGroup]="form" (ngSubmit)="submit()" class="form">
      <div class="field">
        <label for="title">Titre</label>
        <input id="title" formControlName="title" placeholder="Ex. Plus d'internet" />
      </div>

      <div class="field">
        <label for="description">Votre problème</label>
        <textarea
          id="description"
          formControlName="description"
          rows="5"
          placeholder="Ex. Depuis ce matin plus aucune connexion, je télétravaille..."
        ></textarea>
      </div>

      <label class="check">
        <input type="checkbox" [checked]="useForm()" (change)="toggleForm($event)" />
        Je préfère choisir moi-même la catégorie
      </label>

      @if (useForm()) {
        <div class="field">
          <label for="category">Catégorie</label>
          <select id="category" formControlName="category">
            <option [ngValue]="null" disabled>Choisir une catégorie</option>
            @for (c of categories; track c.value) {
              <option [ngValue]="c.value">{{ c.label }}</option>
            }
          </select>
        </div>
        <label class="check">
          <input type="checkbox" formControlName="blocked" />
          Je suis bloqué dans mon activité
        </label>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }

      <div>
        <button class="btn" type="submit" [disabled]="sending()">
          {{ sending() ? 'Envoi en cours…' : 'Envoyer' }}
        </button>
      </div>
    </form>

    @if (result(); as t) {
      <div class="divider-h"></div>
      <article class="card">
        <span class="muted">Ticket #{{ t.id }} créé</span>
        <h3>{{ t.title }}</h3>
        <div class="meta">
          <app-priority-chip [priority]="t.priority" />
          <span>{{ triageLabels[t.triageSource] }}</span>
        </div>
        @if (t.needsReview) {
          <p style="margin-top: 0.75rem">
            Notre équipe va qualifier votre demande très rapidement.
          </p>
        }
        <p style="margin-top: 1rem">
          <a class="pill violet" routerLink="/client/tickets">Voir mes tickets</a>
        </p>
      </article>
    }
  `,
})
export class NewTicket {
  private api = inject(TicketApi);

  protected useForm = signal(false);
  protected sending = signal(false);
  protected result = signal<Ticket | null>(null);
  protected error = signal<string | null>(null);

  protected triageLabels = TRIAGE_LABELS;
  protected categories = Object.entries(CATEGORY_LABELS).map(([value, label]) => ({
    value: value as TicketCategory,
    label,
  }));

  protected form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    category: new FormControl<TicketCategory | null>(null),
    blocked: new FormControl(false, { nonNullable: true }),
  });

  toggleForm(event: Event): void {
    this.useForm.set((event.target as HTMLInputElement).checked);
  }

  submit(): void {
    const v = this.form.getRawValue();
    this.error.set(null);

    if (!v.title.trim()) {
      this.error.set('Le titre est obligatoire.');
      return;
    }
    if (!this.useForm() && !v.description.trim()) {
      this.error.set('Décrivez votre problème.');
      return;
    }
    if (this.useForm() && !v.category) {
      this.error.set('Choisissez une catégorie.');
      return;
    }

    // Texte libre -> le LLM trie ; formulaire -> les regles directement
    const body: CreateTicketRequest = this.useForm()
      ? {
          title: v.title,
          description: v.description || undefined,
          category: v.category!,
          blocked: v.blocked,
        }
      : { title: v.title, description: v.description };

    this.sending.set(true);
    this.api.create(body).subscribe({
      next: (ticket) => {
        this.result.set(ticket);
        this.sending.set(false);
        this.form.reset();
      },
      error: (e) => {
        this.error.set(e.error?.message ?? `Erreur ${e.status}`);
        this.sending.set(false);
      },
    });
  }
}
