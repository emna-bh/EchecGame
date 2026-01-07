import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';

export interface WsMessage {
  type: string;
  gameId?: number;
  moveNumber?: number;
  from?: string;
  to?: string;
  piece?: string;
  byUserId?: number;
  message?: string;
  users?: unknown;
  fromUserId?: number;
  fromUsername?: string;
  accepted?: boolean;
  toUserId?: number;
  toUsername?: string;
  winnerUserId?: number;
  endReason?: string;
}

@Injectable({ providedIn: 'root' })
export class WsService {
  private socket: WebSocket | null = null;
  private messagesSubject = new Subject<WsMessage>();
  messages$ = this.messagesSubject.asObservable();

  constructor(private auth: AuthService, private zone: NgZone) {}

  connect(): void {
    const token = this.auth.token;
    if (!token || this.socket) {
      return;
    }
    const url = `ws://localhost:8080/ws?token=${encodeURIComponent(token)}`;
    this.socket = new WebSocket(url);
    this.socket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data) as WsMessage;
        this.zone.run(() => this.messagesSubject.next(parsed));
      } catch {
        // ignore invalid payloads
      }
    };
    this.socket.onclose = () => {
      this.socket = null;
    };
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  invite(toUserId: number): void {
    this.send({ type: 'invite', toUserId });
  }

  respondInvite(fromUserId: number, accepted: boolean): void {
    this.send({ type: 'invite_response', fromUserId, accepted });
  }

  sendMove(gameId: number, from: string, to: string, piece: string): void {
    this.send({ type: 'move', gameId, from, to, piece });
  }

  resign(gameId: number): void {
    this.send({ type: 'resign', gameId });
  }

  private send(payload: WsMessage): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {      
      return;
    }
    console.log("sending socket: ", payload);
    this.socket.send(JSON.stringify(payload));
  }
}
