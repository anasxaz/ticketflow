import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './core/auth/auth.service';
import { roleGuard } from './core/auth/role.guard';
import { NewTicket } from './features/client/new-ticket';
import { MyTickets } from './features/client/my-tickets';
import { Notifications } from './features/client/notifications';
import { AgentTickets } from './features/agent/agent-tickets';

export const routes: Routes = [
  // Page d'accueil selon le role
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => (inject(AuthService).hasRole('AGENT') ? 'agent' : 'client/tickets'),
  },

  {
    path: 'client',
    canActivate: [roleGuard('CLIENT')],
    children: [
      { path: 'new', component: NewTicket },
      { path: 'tickets', component: MyTickets },
      { path: 'notifications', component: Notifications },
    ],
  },

  { path: 'agent', canActivate: [roleGuard('AGENT')], component: AgentTickets },

  { path: '**', redirectTo: '' },
];
