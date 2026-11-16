import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected auth = inject(AuthService);
  private http = inject(HttpClient);

  protected notifications = signal<unknown[] | null>(null);
  protected error = signal<string | null>(null);

  loadNotifications(): void {
    this.http.get<unknown[]>('/api/notifications').subscribe({
      next: (list) => { this.notifications.set(list); this.error.set(null); },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }
}
