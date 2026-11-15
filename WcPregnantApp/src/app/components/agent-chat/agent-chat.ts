import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgentService } from '../../services/agent.service';
import { AgentChatResponse, AgentStep, AgentToolCallTrace, AgentToolResult } from '../../models/agent.model';
import { GeoPosition, Toilet } from '../../models/toilet.model';

/** Libellés lisibles des outils du registre, pour rendre la boucle agentique transparente. */
const TOOL_LABELS: Record<string, string> = {
  search_toilettes_proches: 'Recherche géospatiale des équipements',
  get_details_toilette: 'Consultation de la fiche d\'un équipement',
  gerer_equipement_crud: 'Gestion des équipements (rôle ADMIN)'
};

/** Requêtes types proposées pour démarrer une conversation. */
const SUGGESTIONS: string[] = [
  'Quelles toilettes accessibles sont les plus proches de moi ?',
  'Y a-t-il des toilettes avec table à langer dans un rayon de 500 m ?',
  'Donne-moi les détails de la toilette la plus proche'
];
/** Libellés des étapes génériques de la boucle agentique (avant la trace des outils). */
const POSITION_STEP_LABEL = 'Position de recherche transmise au contexte';
const MODEL_STEP_LABEL = 'Interrogation du modèle d\'inférence local';
const TOOLS_STEP_LABEL = 'Exécution des outils sélectionnés';


/**
 * Assistant conversationnel branché sur `POST /agent/chat`.
 *
 * Le composant ne contient aucune logique métier : il délègue au service
 * `AgentService`, rend lisible la boucle Tool Calling (position transmise,
 * outils exécutés, synthèse du modèle) et expose les équipements trouvés via
 * `results` pour synchroniser la liste et la carte Leaflet.
 */
@Component({
  selector: 'wc-agent-chat',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './agent-chat.html',
  styleUrl: './agent-chat.css'
})
export class AgentChatComponent {
  /** Position de recherche injectée dans le contexte du modèle local. */
  @Input() position: GeoPosition | null = null;

  /** Équipements détectés par les outils de l'agent. */
  @Output() results = new EventEmitter<Toilet[]>();

  /** Équipement choisi depuis une carte de résultat. */
  @Output() toiletSelected = new EventEmitter<Toilet>();

  question = '';
  steps: AgentStep[] = [];
  answer: string | null = null;
  toilets: Toilet[] = [];
  model: string | null = null;
  busy = false;
  error: string | null = null;
  open = false;
  readonly suggestions: string[] = SUGGESTIONS;

  constructor(private agent: AgentService) {}

  /** Ouvre ou ferme le panneau de conversation. */
  toggle(): void {
    this.open = !this.open;
  }

  /**
   * Envoie la question saisie à l'agent local.
   *
   * <p>Les erreurs d'inférence (503) ou de rôle (403) sont captées et affichées
   * dans le panneau : le composant ne propage jamais d'exception.</p>
   */
  ask(): void {
    const message = this.question.trim();

    if (message.length === 0) {
      this.error = 'Posez une question pour interroger l\'assistant.';

      return;
    }

    this.busy = true;
    this.error = null;
    this.answer = null;
    this.toilets = [];
    this.steps = this.pendingSteps();

    this.agent.chat({ message, position: this.position ?? undefined }).subscribe({
      next: (response: AgentChatResponse) => {
        this.answer = response.answer ?? null;
        this.model = response.model ?? null;
        this.steps = this.completedSteps(response);
        this.toilets = this.extractToilets(response.toolCalls ?? []);
        this.busy = false;

        if (this.toilets.length > 0) {
          this.results.emit(this.toilets);
        }
      },
      error: (err: unknown) => {
        this.error = this.errorMessage(err);
        this.steps = [{ label: 'Inférence locale indisponible', detail: this.error, status: 'error' }];
        this.busy = false;
      }
    });
  }

  /**
   * Envoie une des suggestions proposées.
   *
   * @param question requête prête à l'emploi
   */
  askQuestion(question: string): void {
    this.question = question;
    this.ask();
  }

  /**
   * Signale le choix d'un équipement trouvé par l'agent.
   *
   * @param toilet équipement sélectionné
   */
  select(toilet: Toilet): void {
    this.toiletSelected.emit(toilet);
  }

  /**
   * Formate la distance renvoyée par l'API (en mètres) de façon lisible.
   *
   * @param toilet équipement dont la distance est calculée
   * @returns ex. `120 m` ou `1,30 km`, ou `distance inconnue`
   */
  formatDistance(toilet: Toilet): string {
    const distance = toilet.distance;

    if (distance === undefined || distance === null) {
      return 'distance inconnue';
    }

    return distance < 1000 ? `${Math.round(distance)} m` : `${(distance / 1000).toFixed(2)} km`;
  }

  /**
   * Étapes affichées pendant que le modèle local réfléchit.
   *
   * @returns la trame de la boucle agentique
   */
  private pendingSteps(): AgentStep[] {
    const position = this.position
      ? `${this.position.latitude}, ${this.position.longitude}`
      : 'aucune position fournie';

    return [
      { label: POSITION_STEP_LABEL, detail: position, status: 'done' },
      { label: MODEL_STEP_LABEL, status: 'running' },
      { label: TOOLS_STEP_LABEL, status: 'running' }
    ];
  }

  /**
   * Reconstruit le fil de raisonnement à partir de la trace renvoyée par le serveur.
   *
   * @param response réponse agentique complète
   * @returns la position transmise, l'interrogation du modèle, une étape par outil
   *          exécuté, puis la synthèse du modèle
   */
  private completedSteps(response: AgentChatResponse): AgentStep[] {
    const steps: AgentStep[] = this.pendingSteps()
      .filter(step => step.label !== TOOLS_STEP_LABEL)
      .map(step => ({ ...step, status: 'done' }));

    (response.toolCalls ?? []).forEach(trace => steps.push(this.stepFrom(trace)));

    steps.push({
      label: 'Synthèse de la réponse par le modèle local',
      detail: `${response.iterations ?? 0} itération(s) · ${response.elapsedMillis ?? 0} ms`,
      status: 'done'
    });

    return steps;
  }

  /**
   * Transforme une trace d'appel d'outil en étape lisible.
   *
   * @param trace trace d'un outil intercepté et exécuté côté serveur
   * @returns l'étape correspondante, marquée en erreur si l'outil a été refusé
   */
  private stepFrom(trace: AgentToolCallTrace): AgentStep {
    const label = TOOL_LABELS[trace.tool] ?? `Exécution de l'outil ${trace.tool}`;

    if (trace.status === 'ERROR') {
      return { label, detail: trace.message ?? 'outil refusé', status: 'error' };
    }

    return { label, detail: this.describe(trace), status: 'done' };
  }

  /**
   * Résumé de l'exécution d'un outil : arguments utiles et volume de résultats.
   *
   * @param trace trace de l'appel
   * @returns texte court destiné au fil de l'agent
   */
  private describe(trace: AgentToolCallTrace): string {
    const toolArguments = trace.arguments ?? {};
    const details: string[] = [];

    const radius = this.asNumber(toolArguments['rayon_metres']);
    if (radius !== null) {
      details.push(`rayon ${radius} m`);
    }

    const action = toolArguments['action'];
    if (action !== undefined) {
      details.push(`action ${String(action)}`);
    }

    const id = toolArguments['id'];
    if (id !== undefined) {
      details.push(`id ${String(id)}`);
    }

    details.push(`${trace.result?.count ?? 0} résultat(s)`);

    if (trace.result?.warning) {
      details.push(trace.result.warning);
    }

    return details.join(' · ');
  }

  /**
   * Rassemble les équipements renvoyés par les outils, sans doublon.
   *
   * @param traces traces des outils exécutés pendant la conversation
   * @returns la liste des équipements détectés, triée par l'agent
   */
  private extractToilets(traces: AgentToolCallTrace[]): Toilet[] {
    const found: Toilet[] = [];

    traces.forEach(trace => {
      if (trace.status === 'ERROR') {
        return;
      }

      this.toiletsFrom(trace.result).forEach(toilet => {
        const isDuplicate = found.some(existing => existing.id !== undefined && existing.id === toilet.id);

        if (!isDuplicate) {
          found.push(toilet);
        }
      });
    });

    return found;
  }

  /**
   * Extrait les équipements du champ `data` d'un résultat d'outil
   * (tableau pour la recherche, objet unique pour la fiche).
   *
   * @param result résultat brut de l'outil
   * @returns les équipements compatibles avec le modèle `Toilet`
   */
  private toiletsFrom(result: AgentToolResult | undefined): Toilet[] {
    if (!result) {
      return [];
    }

    const data = result.data;

    if (Array.isArray(data)) {
      return data.filter(item => this.isToilet(item));
    }

    return this.isToilet(data) ? [data] : [];
  }

  /**
   * Vérifie qu'un payload d'outil est un équipement exploitable sur la carte.
   *
   * @param value valeur inconnue renvoyée par l'agent
   * @returns `true` si la structure est compatible avec `Toilet`
   */
  private isToilet(value: unknown): value is Toilet {
    if (!value || typeof value !== 'object') {
      return false;
    }

    const candidate = value as Partial<Toilet>;

    return typeof candidate.libelle === 'string'
      && !!candidate.position
      && typeof candidate.position.latitude === 'number'
      && typeof candidate.position.longitude === 'number';
  }

  /**
   * Convertit un argument d'outil en nombre, ou `null` s'il est absent.
   *
   * @param value valeur de l'argument
   * @returns la valeur numérique, `null` sinon
   */
  private asNumber(value: unknown): number | null {
    return typeof value === 'number' ? value : null;
  }

  /**
   * Message d'erreur adapté au code renvoyé par l'API agentique.
   *
   * @param error erreur interceptée par `HttpClient`
   * @returns libellé affichable dans le panneau
   */
  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object') {
      const status = (error as { status?: number }).status;
      const message = (error as { message?: string }).message;

      if (status === 403) {
        return 'Le rôle ADMIN est requis pour cette opération.';
      }

      if (message) {
        return message;
      }
    }

    return 'Le serveur d\'inférence local est injoignable.';
  }
}



