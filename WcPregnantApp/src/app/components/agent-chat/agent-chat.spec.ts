import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AgentChatComponent } from './agent-chat';
import { AgentService } from '../../services/agent.service';
import { AgentChatResponse } from '../../models/agent.model';
import { Toilet } from '../../models/toilet.model';

describe('AgentChatComponent', () => {
  let fixture: ComponentFixture<AgentChatComponent>;
  let component: AgentChatComponent;
  let agentMock: { chat: jasmine.Spy; getTools: jasmine.Spy };

  const toiletNear: Toilet = {
    id: 1,
    libelle: 'WC de la Mairie',
    distance: 120,
    adress: { voie: 'Rue de la Mairie', codePostal: '35000', ville: 'Rennes' },
    position: { latitude: 48.112, longitude: -1.675 }
  };

  const toiletFar: Toilet = {
    id: 2,
    libelle: 'WC de la Gare',
    distance: 1500,
    adress: { codePostal: '35000' },
    position: { latitude: 48.12, longitude: -1.68 }
  };

  const chatResponse: AgentChatResponse = {
    answer: 'Deux toilettes accessibles ont été trouvées.',
    model: 'Qwen-3.8-Flash-Next',
    iterations: 2,
    elapsedMillis: 420,
    toolCalls: [
      {
        tool: 'search_toilettes_proches',
        status: 'OK',
        arguments: { latitude: 48.1119, longitude: -1.6745, rayon_metres: 500 },
        result: { tool: 'search_toilettes_proches', status: 'OK', count: 2, data: [toiletNear, toiletFar] }
      }
    ]
  };

  beforeEach(() => {
    agentMock = {
      chat: jasmine.createSpy('chat').and.returnValue(of(chatResponse)),
      getTools: jasmine.createSpy('getTools').and.returnValue(of([]))
    };

    TestBed.configureTestingModule({
      imports: [AgentChatComponent],
      providers: [{ provide: AgentService, useValue: agentMock }]
    });

    fixture = TestBed.createComponent(AgentChatComponent);
    component = fixture.componentInstance;
    component.position = { latitude: 48.1119, longitude: -1.6745 };
  });

  it('shouldCreate', () => {
    expect(component).toBeTruthy();
  });

  it('toggle_shouldRenderPanel_whenLauncherIsClicked', () => {
    fixture.detectChanges();
    const native = fixture.nativeElement as HTMLElement;

    expect(native.querySelector('.agent-panel')).toBeNull();

    component.toggle();
    fixture.detectChanges();

    expect(native.querySelector('.agent-panel')).toBeTruthy();
  });

  it('ask_shouldSendQuestionAndPosition_whenQuestionIsFilled', () => {
    component.question = '  toilettes accessibles  ';
    component.ask();

    expect(agentMock.chat).toHaveBeenCalledWith({
      message: 'toilettes accessibles',
      position: { latitude: 48.1119, longitude: -1.6745 }
    });
  });

  it('ask_shouldNotCallAgent_whenQuestionIsBlank', () => {
    component.question = '   ';
    component.ask();

    expect(agentMock.chat).not.toHaveBeenCalled();
    expect(component.error).toContain('Posez une question');
  });

  it('ask_shouldTraceToolCalls_whenAgentExecutesTools', () => {
    component.question = 'toilettes proches';
    component.ask();

    expect(component.steps.length).toBe(4);
    expect(component.steps[0].label).toContain('Position de recherche');
    expect(component.steps[1].label).toContain('Interrogation du modèle');
    expect(component.steps[2].label).toContain('Recherche géospatiale');
    expect(component.steps[2].detail).toContain('rayon 500 m');
    expect(component.steps[2].detail).toContain('2 résultat(s)');
    expect(component.steps[3].label).toContain('Synthèse');
    expect(component.steps[3].detail).toBe('2 itération(s) · 420 ms');
    expect(component.steps.every(step => step.status === 'done')).toBeTrue();
  });

  it('ask_shouldExtractToiletCards_whenSearchToolReturnsData', () => {
    component.question = 'toilettes proches';
    component.ask();

    expect(component.toilets).toEqual([toiletNear, toiletFar]);
    expect(component.answer).toBe('Deux toilettes accessibles ont été trouvées.');
    expect(component.model).toBe('Qwen-3.8-Flash-Next');
    expect(component.busy).toBeFalse();
  });

  it('ask_shouldEmitResults_whenAgentFindsToilets', () => {
    const emitted: Toilet[][] = [];
    component.results.subscribe(value => emitted.push(value));

    component.question = 'toilettes proches';
    component.ask();

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual([toiletNear, toiletFar]);
  });

  it('ask_shouldNotEmitResults_whenAgentFindsNothing', () => {
    agentMock.chat.and.returnValue(of({
      answer: 'Aucune toilette dans ce rayon.',
      toolCalls: [{ tool: 'search_toilettes_proches', status: 'OK', result: { count: 0, data: [] } }]
    }));

    const emitted: Toilet[][] = [];
    component.results.subscribe(value => emitted.push(value));

    component.question = 'à 10 m';
    component.ask();

    expect(component.toilets).toEqual([]);
    expect(emitted.length).toBe(0);
  });

  it('ask_shouldMarkStepAsError_whenToolIsRefused', () => {
    agentMock.chat.and.returnValue(of({
      answer: 'Je ne peux pas créer un équipement.',
      toolCalls: [{ tool: 'gerer_equipement_crud', status: 'ERROR', message: 'ADMIN role required' }]
    }));

    component.question = 'ajoute une toilette';
    component.ask();

    const toolStep = component.steps.find(step => step.label.includes('Gestion des équipements'));

    expect(toolStep?.status).toBe('error');
    expect(toolStep?.detail).toBe('ADMIN role required');
  });

  it('ask_shouldReportError_whenLocalInferenceIsUnreachable', () => {
    agentMock.chat.and.returnValue(throwError({ status: 503, message: 'Local inference server is unreachable' }));

    component.question = 'bonjour';
    component.ask();

    expect(component.error).toBe('Local inference server is unreachable');
    expect(component.busy).toBeFalse();
    expect(component.steps[0].status).toBe('error');
  });

  it('ask_shouldExplainMissingRole_whenApiReturnsForbidden', () => {
    agentMock.chat.and.returnValue(throwError({ status: 403 }));

    component.question = 'supprime la toilette 3';
    component.ask();

    expect(component.error).toBe('Le rôle ADMIN est requis pour cette opération.');
  });

  it('askQuestion_shouldUseSuggestion_whenChipIsClicked', () => {
    component.askQuestion(component.suggestions[0]);

    expect(component.question).toBe(component.suggestions[0]);
    expect(agentMock.chat).toHaveBeenCalled();
  });

  it('select_shouldEmitSelectedToilet_whenCardIsClicked', () => {
    const emitted: Toilet[] = [];
    component.toiletSelected.subscribe(value => emitted.push(value));

    component.select(toiletNear);

    expect(emitted).toEqual([toiletNear]);
  });

  it('formatDistance_shouldSwitchToKilometers_whenDistanceExceedsOneThousand', () => {
    const withoutDistance: Toilet = {
      libelle: 'WC sans distance',
      adress: { codePostal: '35000' },
      position: { latitude: 0, longitude: 0 }
    };

    expect(component.formatDistance(toiletNear)).toBe('120 m');
    expect(component.formatDistance(toiletFar)).toBe('1.50 km');
    expect(component.formatDistance(withoutDistance)).toBe('distance inconnue');
  });
});

