/** "AppNotification" pour ne pas confondre avec "Notification" du navigateur. */
export interface AppNotification {
  id: number;
  eventId: string;
  ticketId: number;
  clientId: number;
  message: string;
  createdAt: string;
}
