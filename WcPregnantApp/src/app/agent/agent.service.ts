import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AgentRequest, AgentResponse } from './agent.models';

/**
 * Service d'accès à l'API agentique (Tool Calling) du service Spring Boot.
 *
 * <p>Il ne contient aucune logique métier : il transmet la requête de
 * l'utilisateur à l'endpoint de conversation et renvoie la réponse du serveur.
 * La base de l'URL est issue de l'environnement (proxy Angular), jamais codée
 * en dur : avec la configuration actuelle (`agentApiUrl: '/agent'`), la
 * requête part sur `/agent/chat`, l'équivalent de `/api/agent/chat` derrière
 * la passerelle.</p>
 */
@Injectable({ providedIn: 'root' })
export class AgentService {
  /** Base des endpoints agentiques : environnement, sinon préfixe `/api/agent`. */
  private static readonly AGENT_BASE_URL: string = environment.agentApiUrl || '/api/agent';

  constructor(private http: HttpClient) {}

  /**
   * Interroge l'agent en langage naturel.
   *
   * @param request question, et position de recherche facultative
   * @returns la réponse de l'agent : synthèse, étapes et toilettes trouvées
   * @throws HttpError si l'endpoint de conversation est injoignable
   */
  askAgent(request: AgentRequest): Observable<AgentResponse> {
    return this.http.post<AgentResponse>(`${AgentService.AGENT_BASE_URL}/chat`, request);
  }
}
