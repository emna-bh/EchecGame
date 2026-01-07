# Test Full Stack - Jeu d'echecs multijoueurs

Prototype Angular + Spring Boot avec WebSockets et persistance minimale des coups.

## Prerequis

- Java 17+
- Node.js 20+

## Demarrage rapide

### Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

Le serveur demarre sur `http://localhost:8080`.

### Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

L'application demarre sur `http://localhost:4200`.

## Architecture

- Frontend Angular (SPA) pour l'auth, le lobby et le plateau.
- Backend Spring Boot REST + WebSocket pour temps reel et persistance.
- Base H2 en memoire pour stocker les coups.

## Fonctionnalites principales

- Creation de compte et connexion
- Liste des joueurs en ligne
- Invitation, acceptation, refus
- Creation de partie a l'acceptation
- Plateau 8x8 et synchro des coups en temps reel
- Sauvegarde de chaque coup en DB (historique)
- Reprise de partie apres reconnexion
- Relecture de partie (play/pause/etapes)
- Validation simple des mouvements
- Abandon (victoire attribuee a l'adversaire)

## Guide utilisateur

1. Creez un compte puis connectez-vous.
2. Ouvrez un second navigateur (ou une fenetre privee) et connectez un autre compte.
3. Depuis le lobby, invitez un joueur en ligne.
4. Le joueur invite accepte: la partie demarre.
5. Jouez en cliquant une piece puis une case de destination.
6. Consultez l'historique et utilisez les controles de relecture.
7. Cliquez "Abandonner" pour quitter la partie.

## Notes techniques

- WebSocket: `ws://localhost:8080/ws?token=...`
- REST: `http://localhost:8080/api/...`
- DB H2 en memoire (reset au redemarrage).
