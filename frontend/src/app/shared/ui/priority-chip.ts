import { Component, computed, input } from '@angular/core';
import { Priority } from '../models/ticket.model';
import { PRIORITY_LABELS } from '../labels';

@Component({
  selector: 'app-priority-chip',
  template: `<span class="chip" [class]="priority().toLowerCase()">{{ label() }}</span>`,
  styles: [
    `
      .chip {
        --c: var(--bright-blue);
        display: inline-block;
        padding: 0.125rem 0.625rem;
        border-radius: 2.75rem;
        font: 600 0.75rem/1.25rem var(--font);
        color: var(--c);
        background: color-mix(in srgb, var(--c) 10%, transparent);
      }
      .critical {
        --c: var(--hot-red);
      }
      .high {
        --c: var(--orange-red);
      }
      .medium {
        --c: var(--electric-violet);
      }
      .low {
        --c: var(--bright-blue);
      }
    `,
  ],
})
export class PriorityChip {
  priority = input.required<Priority>();
  protected label = computed(() => PRIORITY_LABELS[this.priority()]); // recalcule si la priorite change
}
