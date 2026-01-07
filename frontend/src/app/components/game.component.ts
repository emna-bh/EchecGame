import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService, GameStateDto, MoveDto } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { WsService, WsMessage } from '../services/ws.service';

interface Square {
  file: string;
  rank: number;
}

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game.component.html',
  styleUrl: './game.component.css'
})
export class GameComponent implements OnInit, OnDestroy {
  gameId = 0;
  board: string[][] = [];
  moves: MoveDto[] = [];
  selected: Square | null = null;
  myColor: 'white' | 'black' | null = null;
  status = '';
  gameOver = false;
  gameOverMessage = '';
  toast = '';
  toastIcon = '';
  toastType: 'win' | 'lose' | '' = '';
  files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  ranks = [8, 7, 6, 5, 4, 3, 2, 1];
  replayIndex = 0;
  isReplaying = false;
  replaySpeedMs = 700;
  private replayTimer: number | null = null;
  private sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private auth: AuthService,
    private ws: WsService,
    private router: Router,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {
    this.board = this.createInitialBoard();
  }

  ngOnInit(): void {
    this.ws.connect();
    this.sub.add(this.ws.messages$.subscribe((msg) => this.handleMessage(msg)));
    this.sub.add(
      this.route.paramMap.subscribe((params) => {
        const id = Number(params.get('id'));
        if (!id) {
          this.router.navigateByUrl('/lobby');
          return;
        }
        this.gameId = id;
        this.loadGame();
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
    this.clearReplayTimer();
  }

  selectSquare(file: string, rank: number): void {
    if (!this.isLiveView()) {
      return;
    }
    if (this.gameOver) {
      return;
    }
    const piece = this.getPiece(file, rank);
    if (!this.selected) {
      if (piece && this.isOwnPiece(piece) && this.isMyTurn()) {
        this.selected = { file, rank };
      }
      return;
    }
    const from = this.selected;
    this.selected = null;
    if (!this.isMyTurn()) {
      return;
    }
    const movingPiece = this.getPiece(from.file, from.rank);
    if (!movingPiece) {
      return;
    }
    const toSquare = `${file}${rank}`;
    const fromSquare = `${from.file}${from.rank}`;
    if (fromSquare === toSquare) {
      return;
    }
    this.ws.sendMove(this.gameId, fromSquare, toSquare, movingPiece);
  }

  isSelected(file: string, rank: number): boolean {
    return this.selected?.file === file && this.selected?.rank === rank;
  }

  getPiece(file: string, rank: number): string {
    const coords = this.squareToCoords(file, rank);
    return this.board[coords.row][coords.col];
  }

  pieceLabel(piece: string): string {
    if (!piece) {
      return '';
    }
    const map: Record<string, string> = {
      wK: '♔',
      wQ: '♕',
      wR: '♖',
      wB: '♗',
      wN: '♘',
      wP: '♙',
      bK: '♚',
      bQ: '♛',
      bR: '♜',
      bB: '♝',
      bN: '♞',
      bP: '♟︎'
    };
    return map[piece] ?? '';
  }

  pieceLetter(piece: string): string {
    if (!piece || piece.length < 2) {
      return '';
    }
    const letter = piece.charAt(1);
    return letter === 'P' ? 'P' : letter;
  }

  trackByIndex(index: number): number {
    return index;
  }

  startReplay(): void {
    if (this.isReplaying) {
      return;
    }
    if (this.replayIndex >= this.moves.length) {
      this.replayIndex = 0;
      this.renderBoard(this.replayIndex);
    }
    this.isReplaying = true;
    this.clearReplayTimer();
    this.replayTimer = window.setInterval(() => {
      if (this.replayIndex < this.moves.length) {
        this.replayIndex += 1;
        this.renderBoard(this.replayIndex);
      } else {
        this.pauseReplay();
      }
    }, this.replaySpeedMs);
  }

  pauseReplay(): void {
    this.isReplaying = false;
    this.clearReplayTimer();
  }

  stepForward(): void {
    if (this.replayIndex < this.moves.length) {
      this.replayIndex += 1;
      this.renderBoard(this.replayIndex);
    }
  }

  stepBack(): void {
    if (this.replayIndex > 0) {
      this.replayIndex -= 1;
      this.renderBoard(this.replayIndex);
    }
  }

  resetReplay(): void {
    this.pauseReplay();
    this.replayIndex = 0;
    this.renderBoard(this.replayIndex);
  }

  exitReplay(): void {
    this.pauseReplay();
    this.replayIndex = this.moves.length;
    this.renderBoard(this.replayIndex);
  }

  onReplayIndexChange(value: number): void {
    this.replayIndex = value;
    this.renderBoard(this.replayIndex);
  }

  isLiveView(): boolean {
    return !this.isReplaying && this.replayIndex === this.moves.length;
  }

  private handleMessage(message: WsMessage): void {
    this.zone.run(() => {
      if (message.type === 'move' && Number(message.gameId) === this.gameId) {
        const move: MoveDto = {
          id: 0,
          moveNumber: Number(message.moveNumber),
          fromSquare: String(message.from),
          toSquare: String(message.to),
          piece: String(message.piece),
          byUserId: Number(message.byUserId),
          createdAt: new Date().toISOString()
        };
        this.applyMove(move);
        this.cdr.detectChanges();
      }
      if (message.type === 'game_over' && Number(message.gameId) === this.gameId) {
        this.gameOver = true;
        const winnerId = Number(message.winnerUserId);
        const isWinner = this.auth.user?.userId === winnerId;
        this.gameOverMessage = isWinner ? 'Victoire' : 'Defaite';
        const reason = message.endReason === 'resign' ? 'abandon' : 'fin de partie';
        this.toast = isWinner
          ? `Victoire: l'adversaire a ${reason}.`
          : `Defaite: vous avez ${reason}.`;
        this.toastIcon = isWinner ? '🏆' : '☹';
        this.toastType = isWinner ? 'win' : 'lose';
        this.scheduleExit();
        this.cdr.detectChanges();
      }
      if (message.type === 'error') {
        this.status = String(message.message || 'Erreur serveur');
        this.cdr.detectChanges();
      }
    });
  }

  private loadGame(): void {
    this.board = this.createInitialBoard();
    this.moves = [];
    this.replayIndex = 0;
    this.gameOver = false;
    this.gameOverMessage = '';
    this.api.getActiveGame().subscribe({
      next: (state) => {
        if (state && state.gameId === this.gameId) {
          this.setPlayerColor(state);
          this.applyMoves(state.moves);
          if (state.status === 'FINISHED') {
            this.gameOver = true;
            this.gameOverMessage = state.winnerUserId === this.auth.user?.userId ? 'Victoire' : 'Defaite';
          }
        } else {
          this.api.getMoves(this.gameId).subscribe({
            next: (moves) => this.applyMoves(moves),
            error: () => (this.status = 'Impossible de charger la partie')
          });
        }
      },
      error: () => (this.status = 'Impossible de charger la partie')
    });
  }

  private setPlayerColor(state: GameStateDto): void {
    const userId = this.auth.user?.userId;
    if (!userId) {
      this.myColor = null;
      return;
    }
    this.myColor = userId === state.whiteUserId ? 'white' : 'black';
  }

  private applyMoves(moves: MoveDto[]): void {
    moves.forEach((move) => this.applyMove(move));
  }

  private applyMove(move: MoveDto): void {
    this.moves = [...this.moves, move].sort((a, b) => a.moveNumber - b.moveNumber);
    if (!this.isReplaying) {
      this.replayIndex = this.moves.length;
      this.renderBoard(this.replayIndex);
    }
    this.status = '';
  }

  private isOwnPiece(piece: string): boolean {
    return this.myColor === 'white' ? piece.startsWith('w') : piece.startsWith('b');
  }

  isMyTurn(): boolean {
    if (!this.myColor) {
      return false;
    }
    if (this.gameOver) {
      return false;
    }
    const whiteTurn = this.moves.length % 2 === 0;
    return (whiteTurn && this.myColor === 'white') || (!whiteTurn && this.myColor === 'black');
  }

  resign(): void {
    if (this.gameOver) {
      return;
    }
    this.ws.resign(this.gameId);
    this.gameOver = true;
    this.gameOverMessage = 'Abandon';
    this.toast = 'Vous avez abandonne la partie.';
    this.toastIcon = '☹';
    this.toastType = 'lose';
    this.scheduleExit();
  }

  private scheduleExit(): void {
    window.setTimeout(() => {
      this.router.navigateByUrl('/lobby');
    }, 3000);
  }

  private createInitialBoard(): string[][] {
    const emptyRow = () => Array(8).fill('');
    const board = Array.from({ length: 8 }, emptyRow);

    board[0] = ['bR', 'bN', 'bB', 'bQ', 'bK', 'bB', 'bN', 'bR'];
    board[1] = Array(8).fill('bP');
    board[6] = Array(8).fill('wP');
    board[7] = ['wR', 'wN', 'wB', 'wQ', 'wK', 'wB', 'wN', 'wR'];

    return board;
  }

  private renderBoard(moveCount: number): void {
    this.board = this.createInitialBoard();
    const slice = this.moves.slice(0, moveCount);
    slice.forEach((move) => {
      const from = this.parseSquare(move.fromSquare);
      const to = this.parseSquare(move.toSquare);
      const moving = this.board[from.row][from.col] || move.piece;
      this.board[from.row][from.col] = '';
      this.board[to.row][to.col] = moving;
    });
  }

  private clearReplayTimer(): void {
    if (this.replayTimer !== null) {
      window.clearInterval(this.replayTimer);
      this.replayTimer = null;
    }
  }

  private parseSquare(square: string): { row: number; col: number } {
    const file = square.charAt(0);
    const rank = Number(square.charAt(1));
    return this.squareToCoords(file, rank);
  }

  private squareToCoords(file: string, rank: number): { row: number; col: number } {
    const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
    const col = files.indexOf(file);
    const row = 8 - rank;
    return { row, col };
  }
}
