import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { AgentChatRequest, AgentChatResponse } from '../models/agent.model';

/**
 * Client de l'API agentique (Tool Calling) du service Spring Boot.
 *
 * <p>Le service ne contient aucune logique métier : il transmet la question de
 * l'utilisateur au modèle servi localement par Strata AI et renvoie la réponse
 * du serveur, trace des outils exécutée. L'URL est issue de l'environnement
 * (proxy Angular), jamais codée en dur.</p>
 */
@Injectable({ providedIn: 'root' })
export class AgentService {
  constructor(private http: HttpClient, private auth: AuthService) {}

  /**
   * Interroge l'agent en langage naturel.
   *
   * @param request question, position de recherche et cadrage optionnel
   * @returns la réponse finale du modèle et la trace des outils exécutés
   */
  chat(request: AgentChatRequest): Observable<AgentChatResponse> {
    return this.http.post<AgentChatResponse>(
      `${environment.agentApiUrl}/chat`,
      request,
      { headers: this.headers() }
    );
  }

  /**
   * Liste le registre des outils exposés au modèle (format OpenAI Function Calling).
   *
   * @returns les enveloppes d'outils telles que renvoyées par `/agent/tools`
   */
  getTools(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${environment.agentApiUrl}/tools`, { headers: this.headers() });
  }

  /**
   * En-tête d'authentification Basic, ou objet vide si aucune session n'est ouverte.
   *
   * @returns un objet d'en-têtes prêt pour `HttpClient`
   */
  private headers(): Record<string, string> {
    const header = this.auth.header;

    return header ? { Authorization: header } : {};
  }
}
