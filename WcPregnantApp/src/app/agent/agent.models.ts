/**
 * Contrats de la conversation agentique (`POST /api/agent/chat`).
 *
 * <p>Ces interfaces décrivent la requête envoyée à l'agent local (Strata AI,
 * Qwen-3.8-Flash-Next) et la réponse attendue : la synthèse rédigée par le
 * modèle, la liste des étapes de la boucle Tool Calling et les équipements
 * géolocalisés trouvés par les outils métier.</p>
 */

/**
 * Question posée à l'agent, éventuellement cadrée par la position
 * géographique de l'utilisateur (WGS84).
 */
export interface AgentRequest {
  /** Question ou instruction exprimée en langage naturel. */
  message: string;

  /** Latitude de recherche, facultative (intervalle [-90, 90]). */
  latitude?: number;

  /** Longitude de recherche, facultative (intervalle [-180, 180]). */
  longitude?: number;
}

/**
 * Réponse de l'agent : synthèse textuelle, trace des étapes exécutées
 * et équipements renvoyés par les outils.
 */
export interface AgentResponse {
  /** Réponse rédigée par le modèle local. */
  reply: string;

  /** Étapes de la boucle agentique, dans l'ordre d'exécution. */
  steps: string[];

  /** Équipements (toilettes) remontés par les outils du registre. */
  toilettes: any[];
}
