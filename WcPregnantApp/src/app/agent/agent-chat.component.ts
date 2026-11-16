import { Component, EventEmitter, computed, input, Output } from '@angular/core';
import { signal } from '@angular/core';
import { AgentService } from './agent.service';
import { AgentRequest, AgentResponse } from './agent.models';
import { GeoPosition } from '../models/toilet.model';

/** Étapes affichées pendant l'inférence, avant la réponse de l'agent. */
const PENDING_STEPS: string[] = [
  'Analyse de la position…',
  'Recherche géospatiale des équipements…',
  'Interrogation du modèle local…'
];

/** Questions types proposées pour démarrer une conversation. */
const SUGGESTIONS: string[] = [
  'Quelles toilettes accessibles sont les plus proches de moi ?',
  'Y a-t-il des toilettes avec table à langer dans un rayon de 500 m ?'
];

/**
 * Bulle flottante agentique : un lanceur fixé en bas à droite qui ouvre
 * une fenêtre de conversation branchée sur `POST /agent/chat`.
 *
 * <p>L'état du composant est piloté par des Signals : la question, l'ouverture
 * de la fenêtre, le mode « réflexion » et les résultats sont des `signal`,
 * tandis que la timeline des étapes, le nombre de résultats et la possibilité
 * d'envoyer sont des `computed` dérivés de ces signaux. Le composant ne contient
 * aucune logique métier : il délègue entièrement à `AgentService`.</p>
 */
@Component({
  selector: 'wc-agent-chat-fab',
  standalone: true,
  templateUrl: './agent-chat.component.html',
  styleUrl: './agent-chat.component.scss'
})
export class AgentChatComponent {
  /** Position de recherche injectée dans le contexte du modèle local. */
  position = input<GeoPosition | null>(null);

  /** Équipement choisi depuis une mini-carte de résultat. */
  @Output() selectToilette = new EventEmitter<any>();

  /** Question saisie dans la fenêtre de chat. */
  readonly question = signal('');

  /** État d'ouverture de la fenêtre de conversation. */
  readonly open = signal(false);

  /** `true` pendant l'appel à l'endpoint agentique. */
  readonly busy = signal(false);

  /** Message d'erreur affichable, `null` si aucune erreur. */
  readonly error = signal<string | null>(null);

  /** Synthèse rédigée par le modèle local. */
  readonly reply = signal<string | null>(null);

  /** Étapes réellement renvoyées par l'agent après la réponse. */
  readonly steps = signal<string[]>([]);

  /** Équipements remontés par les outils du registre. */
  readonly toilettes = signal<any[]>([]);

  readonly suggestions: string[] = SUGGESTIONS;

  /** Timeline affichée : étapes génériques pendant la réflexion, étapes de l'agent ensuite. */
  readonly timeline = computed(() => (this.busy() ? PENDING_STEPS : this.steps()));

  /** Nombre d'équipements proposés sous forme de mini-cartes. */
  readonly resultCount = computed(() => this.toilettes().length);

  /** L'envoi n'est possible que si la question est non vide et l'agent disponible. */
  readonly canAsk = computed(() => !this.busy() && this.question().trim().length > 0);

  constructor(private agent: AgentService) {}

  /** Ouvre ou ferme la fenêtre de conversation. */
  toggle(): void {
    this.open.set(!this.open());
  }

  /** Ferme la fenêtre de conversation. */
  close(): void {
    this.open.set(false);
  }

  /**
   * Envoie la question saisie à l'agent local.
   *
   * <p>Les erreurs d'inférence (503) ou de rôle (403) sont captées et affichées
   * dans la fenêtre : le composant ne propage jamais d'exception.</p>
   */
  ask(): void {
    const message = this.question().trim();

    if (message.length === 0) {
      this.error.set('Posez une question pour interroger l\'assistant.');

      return;
    }

    const request: AgentRequest = { message };
    const position = this.position();

    if (position) {
      request.latitude = position.latitude;
      request.longitude = position.longitude;
    }

    this.busy.set(true);
    this.error.set(null);
    this.reply.set(null);
    this.steps.set([]);
    this.toilettes.set([]);

    this.agent.askAgent(request).subscribe({
      next: (response: AgentResponse) => {
        this.reply.set(response.reply ?? null);
        this.steps.set(response.steps ?? []);
        this.toilettes.set(response.toilettes ?? []);
        this.busy.set(false);
      },
      error: (err: Error) => {
        this.error.set(this.errorMessage(err));
        this.busy.set(false);
      }
    });
  }

  /**
   * Envoie une suggestion cliquée comme question à l'agent.
   *
   * @param suggestion question proposée affichée sous forme de puce
   */
  askQuestion(suggestion: string): void {
    this.question.set(suggestion);
    this.ask();
  }

  /**
   * Émet l'équipement sélectionné dans une mini-carte.
   *
   * @param toilet équipement cliqué par l'utilisateur
   */
  select(toilet: any): void {
    this.selectToilette.emit(toilet);
  }

  /**
   * Libellé lisible d'un équipement renvoyé par l'agent.
   *
   * @param toilet payload brut de l'outil
   * @returns le libellé, ou un libellé de repli s'il est absent
   */
  toiletLabel(toilet: any): string {
    return typeof toilet?.libelle === 'string' && toilet.libelle.length > 0
      ? toilet.libelle
      : 'Équipement sans nom';
  }

  /**
   * Formate la distance d'un équipement en mètres ou en kilomètres.
   *
   * @param toilet payload brut de l'outil
   * @returns distance lisible, ou « distance inconnue » si elle est absente
   */
  formatDistance(toilet: any): string {
    const distance = toilet?.distance;

    if (typeof distance !== 'number') {
      return 'distance inconnue';
    }

    return distance < 1000 ? `${distance} m` : `${(distance / 1000).toFixed(2)} km`;
  }

  /**
   * Message d'erreur adapté au code renvoyé par l'API agentique.
   *
   * @param error erreur interceptée par `HttpClient`
   * @returns libellé affichable dans la fenêtre de chat
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
