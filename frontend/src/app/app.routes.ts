import { Routes } from '@angular/router';
import { LoginComponent } from './components/login.component';
import { LobbyComponent } from './components/lobby.component';
import { GameComponent } from './components/game.component';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'lobby', component: LobbyComponent, canActivate: [authGuard] },
  { path: 'game/:id', component: GameComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' }
];
