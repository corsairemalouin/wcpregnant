import { Injectable } from '@angular/core';

/**
 * Préférences de l'application.
 *
 * Le "mode développeur" est la seule préférence persistée : il expose les
 * champs latitude/longitude pour saisir manuellement un point de recherche.
 * Par défaut il est désactivé, la recherche partant de la géolocalisation
 * du navigateur.
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  /** Clé utilisée pour persistér la préférence dans le navigateur. */
  private static readonly STORAGE_KEY = 'wc-pregnant-developer-mode';

  private developerMode = false;

  constructor() {
    this.developerMode = this.readStoredMode();
  }

  /** Indique si la saisie manuelle des coordonnées est autorisée. */
  get isDeveloperMode(): boolean {
    return this.developerMode;
  }

  /**
   * Active ou désactive le mode développeur et persiste le choix.
   *
   * @param enabled true pour afficher les champs latitude/longitude
   */
  setDeveloperMode(enabled: boolean): void {
    this.developerMode = enabled;
    this.writeStoredMode(enabled);
  }

  /**
   * Inverse le mode développeur.
   *
   * @returns la valeur du mode après la bascule
   */
  toggleDeveloperMode(): boolean {
    this.setDeveloperMode(!this.developerMode);
    return this.developerMode;
  }

  /**
   * Lit la préférence depuis le `localStorage`, en tolérant son indisponibilité
   * (navigation privée, environnement sans stockage).
   *
   * @returns true si le mode développeur avait été enregistré
   */
  private readStoredMode(): boolean {
    try {
      return localStorage.getItem(SettingsService.STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  }

  /**
   * Enregistre la préférence ; aucune exception n'est propagée si le
   * stockage est inaccessible.
   *
   * @param enabled valeur du mode développeur
   */
  private writeStoredMode(enabled: boolean): void {
    try {
      localStorage.setItem(SettingsService.STORAGE_KEY, String(enabled));
    } catch {
      // Stockage indisponible : la préférence reste uniquement en mémoire.
    }
  }
}
