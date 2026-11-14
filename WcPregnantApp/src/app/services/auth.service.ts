import { Injectable } from '@angular/core';

/**
 * Détient le jeton d'authentification Basic utilisé pour appeler l'API.
 * Les identifiants ne sont jamais stockés dans le code : ils sont saisis
 * par l'utilisateur dans l'interface.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private authorizationHeader: string | null = null;

  /**
   * Mémorise les identifiants sous forme d'en-tête `Authorization: Basic ...`.
   *
   * @param username identifiant utilisateur
   * @param password mot de passe
   */
  login(username: string, password: string): void {
    this.authorizationHeader = `Basic ${btoa(`${username}:${password}`)}`;
  }

  /** Efface le jeton d'authentification. */
  logout(): void {
    this.authorizationHeader = null;
  }

  /** En-tête d'authentification à injecter dans les requêtes, ou `null`. */
  get header(): string | null {
    return this.authorizationHeader;
  }

  /** Indique si l'utilisateur est authentifié. */
  isAuthenticated(): boolean {
    return this.authorizationHeader !== null;
  }
}
