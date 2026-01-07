import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BehaviorSubject, Subscription } from 'rxjs';
import { ApiService, OnlineUser } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { WsService, WsMessage } from '../services/ws.service';

interface Invite {
  fromUserId: number;
  fromUsername: string;
}

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lobby.component.html',
  styleUrl: './lobby.component.css'
})
export class LobbyComponent implements OnInit, OnDestroy {
  onlineUsers$ = new BehaviorSubject<OnlineUser[]>([]);
  invites: Invite[] = [];
  notifications: string[] = [];
  pendingInvites = new Set<number>();
  private sub = new Subscription();

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private ws: WsService,
    private router: Router,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.ws.connect();
    this.sub.add(
      this.ws.messages$.subscribe((message) => this.handleMessage(message))
    );
    this.api.getOnlineUsers().subscribe({
      next: (users) => this.onlineUsers$.next(this.normalizeOnlineUsers(users)),
      error: () => {
        this.onlineUsers$.next([]);
      }
    });
    
    this.api.getActiveGame().subscribe({
      next: (state) => {
        if (state?.gameId) {
          this.router.navigate(['/game', state.gameId]);
        }
      },
      error: () => {
        // ignore resume errors
      }
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  sendInvite(userId: number): void {
    this.pendingInvites.add(userId);
    this.ws.invite(userId);
  }

  acceptInvite(invite: Invite): void {
    this.ws.respondInvite(invite.fromUserId, true);
    this.invites = this.invites.filter((item) => item !== invite);
  }

  declineInvite(invite: Invite): void {
    this.ws.respondInvite(invite.fromUserId, false);
    this.invites = this.invites.filter((item) => item !== invite);
  }

  logout(): void {
    this.auth.logout();
    this.ws.disconnect();
    this.router.navigateByUrl('/login');
  }

  private handleMessage(message: WsMessage): void {
    this.zone.run(() => {
      if (message.type === 'online_users') {
        this.onlineUsers$.next(this.normalizeOnlineUsers(message.users));
        this.cdr.detectChanges();
        return;
      }
      if (message.type === 'invite') {
        const invite = {
          fromUserId: Number(message.fromUserId),
          fromUsername: String(message.fromUsername)
        };
        this.invites = [invite, ...this.invites.filter((item) => item.fromUserId !== invite.fromUserId)];
        this.cdr.detectChanges();
        return;
      }
      if (message.type === 'invite_response' && message.accepted === false) {
        this.notifications = [`Invitation refusee par ${message.fromUserId}`, ...this.notifications].slice(0, 3);
        this.pendingInvites.delete(Number(message.fromUserId));
        this.cdr.detectChanges();
        return;
      }
    if (message.type === 'invite_sent') {
      const label = message.toUsername ? message.toUsername : `#${message.toUserId}`;
      this.notifications = [`Invitation envoyee a ${label}`, ...this.notifications].slice(0, 3);
      this.pendingInvites.add(Number(message.toUserId));
      this.cdr.detectChanges();
      return;
    }
      if (message.type === 'game_start') {
        this.pendingInvites.clear();
      }
      if (message.type === 'error') {
        this.notifications = [`Erreur: ${message.message}`, ...this.notifications].slice(0, 3);
        this.pendingInvites.clear();
        this.cdr.detectChanges();
        return;
      }
      if (message.type === 'game_start') {
        const gameId = Number(message.gameId);
        if (gameId) {
          this.router.navigate(['/game', gameId]);
        }
      }
    });
  }

  get selfId(): number | null {
    return this.auth.user?.userId ?? null;
  }

  private normalizeOnlineUsers(input: unknown): OnlineUser[] {
    if (Array.isArray(input)) {
      return input as OnlineUser[];
    }
    if (input && typeof input === 'object') {
      return Object.values(input as Record<string, OnlineUser>);
    }
    return [];
  }
}
