import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Toilet } from '../models/toilet.model';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Accès aux endpoints `/toilets` du contrat OpenAPI.
 * Toutes les requêtes sont authentifiées en Basic dès qu'un utilisateur est connecté.
 */
@Injectable({ providedIn: 'root' })
export class ToiletService {
  constructor(private http: HttpClient, private auth: AuthService) {}

  /** Options de requête portant l'en-tête d'authentification si disponible. */
  private options(): { headers?: Record<string, string> } {
    const header = this.auth.header;
    return header ? { headers: { Authorization: header } } : {};
  }

  /**
   * Liste toutes les toilettes avec pagination.
   *
   * @param page page à récupérer (0-based)
   * @param size taille de page
   * @returns observable sur la liste des toilettes
   */
  getToilets(page = 0, size = 20): Observable<Toilet[]> {
    return this.http.get<Toilet[]>(`${environment.apiUrl}?page=${page}&size=${size}`, this.options());
  }

  /**
   * Recherche les toilettes les plus proches d'une position.
   * Le contrat OpenAPI expose la proximité via `GET /toilets?latitude=..&longitude=..` :
   * le backend trie par distance et plafonne le nombre de résultats (`app.result.max`).
   *
   * @param lat latitude du point de recherche
   * @param lng longitude du point de recherche
   * @param page page à récupérer (0-based)
   * @param size nombre maximal de résultats demandé
   * @returns observable sur la liste triée par distance croissante
   */
  findNearest(lat: number, lng: number, page = 0, size = 10): Observable<Toilet[]> {
    return this.http.get<Toilet[]>(
      `${environment.apiUrl}?latitude=${lat}&longitude=${lng}&page=${page}&size=${size}`,
      this.options()
    );
  }

  /**
   * Récupère une toilette par son identifiant.
   *
   * @param id identifiant de la toilette
   * @returns observable sur la toilette
   */
  getToilet(id: number): Observable<Toilet> {
    return this.http.get<Toilet>(`${environment.apiUrl}/${id}`, this.options());
  }

  /**
   * Crée une nouvelle toilette.
   *
   * @param toilet payload conforme au DTO `Toilet`
   * @returns observable sur la toilette créée
   */
  createToilet(toilet: Toilet): Observable<Toilet> {
    return this.http.post<Toilet>(environment.apiUrl, toilet, this.options());
  }

  /**
   * Remplace intégralement une toilette existante.
   *
   * @param id identifiant de la toilette
   * @param toilet nouveau payload
   * @returns observable sur la toilette mise à jour
   */
  updateToilet(id: number, toilet: Toilet): Observable<Toilet> {
    return this.http.put<Toilet>(`${environment.apiUrl}/${id}`, toilet, this.options());
  }

  /**
   * Supprime une toilette (réservé au rôle ADMIN).
   *
   * @param id identifiant de la toilette
   * @returns observable vide
   */
  deleteToilet(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/${id}`, this.options());
  }
}
