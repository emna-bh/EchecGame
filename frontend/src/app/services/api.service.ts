import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OnlineUser {
  id: number;
  username: string;
}

export interface MoveDto {
  id: number;
  moveNumber: number;
  fromSquare: string;
  toSquare: string;
  piece: string;
  byUserId: number;
  createdAt: string;
}

export interface GameStateDto {
  gameId: number;
  whiteUserId: number;
  blackUserId: number;
  status: string;
  winnerUserId?: number;
  endReason?: string;
  moves: MoveDto[];
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getOnlineUsers(): Observable<OnlineUser[]> {
    return this.http.get<OnlineUser[]>(`${this.apiUrl}/users/online`);
  }

  getActiveGame(): Observable<GameStateDto | null> {
    return this.http.get<GameStateDto | null>(`${this.apiUrl}/games/active`);
  }

  getMoves(gameId: number): Observable<MoveDto[]> {
    return this.http.get<MoveDto[]>(`${this.apiUrl}/games/${gameId}/moves`);
  }
}
