import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AgentService } from './agent.service';
import { AgentRequest, AgentResponse } from './agent.models';
import { environment } from '../../environments/environment';

describe('AgentService', () => {
  let service: AgentService;

  /** `HttpClient` simulé : seul `post` est exercé par le service. */
  const httpMock = {
    post: jasmine.createSpy('post').and.returnValue(of({ reply: '', steps: [], toilettes: [] }))
  };

  /** URL de conversation attendue : base de l'environnement + `/chat`. */
  const chatUrl = `${environment.agentApiUrl}/chat`;

  /** Réponse type de l'agent : synthèse, étapes et équipements trouvés. */
  const response: AgentResponse = {
    reply: '3 toilettes accessibles trouvées à moins de 400 m.',
    steps: ['Recherche géospatiale des équipements', 'Synthèse du modèle local'],
    toilettes: [{ libelle: 'Toilettes du Parc', distance: 120 }]
  };

  beforeEach(() => {
    httpMock.post.calls.reset();
    httpMock.post.and.returnValue(of(response));

    TestBed.configureTestingModule({
      providers: [{ provide: HttpClient, useValue: httpMock }]
    });

    service = TestBed.inject(AgentService);
  });

  it('askAgent_shouldPostToAgentChatEndpoint_whenCalled', () => {
    // Arrange
    const request: AgentRequest = { message: 'toilettes accessibles proches', latitude: 48.1119, longitude: -1.6745 };

    // Act
    service.askAgent(request).subscribe();

    // Assert
    expect(httpMock.post).toHaveBeenCalledTimes(1);
    expect(httpMock.post.calls.argsFor(0)[0]).toBe(chatUrl);
    expect(httpMock.post.calls.argsFor(0)[1]).toEqual(request);
  });

  it('askAgent_shouldSendOnlyTheMessage_whenPositionIsAbsent', () => {
    // Arrange / Act
    service.askAgent({ message: 'bonjour' }).subscribe();

    // Assert : aucune coordonnée n'est ajoutée au payload
    expect(httpMock.post.calls.argsFor(0)[1]).toEqual({ message: 'bonjour' });
  });

  it('askAgent_shouldEmitReplyStepsAndToilets_whenServerResponds', () => {
    // Arrange
    let emitted: AgentResponse | undefined;

    // Act
    service.askAgent({ message: 'toilettes accessibles' }).subscribe(value => emitted = value);

    // Assert
    expect(emitted).toEqual(response);
  });

  it('askAgent_shouldPropagateHttpError_whenInferenceServerIsUnreachable', () => {
    // Arrange
    const error = { status: 503, message: 'Le serveur d\'inférence local est injoignable' };
    httpMock.post.and.returnValue(throwError(() => error));

    let captured: unknown;

    // Act
    service.askAgent({ message: 'bonjour' }).subscribe({
      next: () => fail('aucune réponse ne doit être émise en cas d\'erreur HTTP'),
      error: err => captured = err
    });

    // Assert
    expect(captured).toEqual(error);
  });

  it('askAgent_shouldUseRelativeUrl_whenProxyIsConfigured', () => {
    // Arrange / Act
    service.askAgent({ message: 'bonjour' }).subscribe();

    // Assert : aucune URL de backend n'est codée en dur, le proxy Angular relaie la requête
    expect(httpMock.post.calls.argsFor(0)[0]).not.toMatch(/^https?:\/\//);
  });
});
