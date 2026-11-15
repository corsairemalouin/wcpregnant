import { GeoPosition } from './toilet.model';

/**
 * Contrats de l'API agentique (`/api/agent/...`).
 *
 * Ils reflètent exactement les schémas `AgentChatRequest`, `AgentChatResponse`,
 * `AgentToolCallTrace` et `ToolResult` déclarés dans
 * `WcPregnantService/src/main/resources/api/AgentAPI.yaml` : le frontend ne
 * doit jamais inventer un champ absent du contrat.
 */

/** Question en langage naturel, enrichie de la position de l'utilisateur. */
export interface AgentChatRequest {
  message: string;
  position?: GeoPosition;
  maxIterations?: number;
}

/** Résultat brut d'un outil du registre (record `ToolResult` côté backend). */
export interface AgentToolResult {
  tool?: string;
  status?: string;
  count?: number;
  data?: unknown;
  warning?: string;
}

/** Trace d'un appel d'outil intercepté puis exécuté par le serveur. */
export interface AgentToolCallTrace {
  tool: string;
  status: string;
  arguments?: Record<string, unknown>;
  result?: AgentToolResult;
  message?: string;
}

/** Réponse finale du modèle local, accompagnée de la trace de la boucle agentique. */
export interface AgentChatResponse {
  answer?: string;
  model?: string;
  iterations?: number;
  toolCalls?: AgentToolCallTrace[];
  elapsedMillis?: number;
}

/** Étape de raisonnement rendue lisible dans le fil de l'assistant. */
export interface AgentStep {
  label: string;
  detail?: string;
  status: 'running' | 'done' | 'error';
}
