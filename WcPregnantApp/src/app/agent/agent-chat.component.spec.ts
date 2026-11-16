import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError, Subject } from 'rxjs';
import { AgentChatComponent } from './agent-chat.component';
import { AgentService } from './agent.service';
import { AgentResponse } from './agent.models';

describe('AgentChatComponent (bulle flottante)', () => {
  let fixture: ComponentFixture<AgentChatComponent>;
  let component: AgentChatComponent;
  let agentMock: { askAgent: jasmine.Spy };

  const toiletNear = {
    id: 1,
    libelle: 'WC de la Mairie',
    distance: 120,
    adress: { voie: 'Rue de la Mairie', codePostal: '35000', ville: 'Rennes' },
    position: { latitude: 48.112, longitude: -1.675 }
  };

  const toiletFar = {
    id: 2,
    libelle: 'WC de la Gare',
    distance: 1500,
    adress: { codePostal: '35000' },
    position: { latitude: 48.12, longitude: -1.68 }
  };

  const chatResponse: AgentResponse = {
    reply: 'Deux toilettes accessibles ont été trouvées.',
    steps: ['Analyse de la position', 'Recherche géospatiale', 'Synthèse du modèle local'],
    toilettes: [toiletNear, toiletFar]
  };

  beforeEach(() => {
    agentMock = { askAgent: jasmine.createSpy('askAgent').and.returnValue(of(chatResponse)) };

    TestBed.configureTestingModule({
      imports: [AgentChatComponent],
      providers: [{ provide: AgentService, useValue: agentMock }]
    });

    fixture = TestBed.createComponent(AgentChatComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('position', { latitude: 48.1119, longitude: -1.6745 });
  });

  it('shouldCreate', () => {
    expect(component).toBeTruthy();
  });

  it('toggle_shouldRenderFloatingWindow_whenBubbleIsClicked', () => {
    fixture.detectChanges();
    const native = fixture.nativeElement as HTMLElement;

    expect(native.querySelector('.agent-window')).toBeNull();

    component.toggle();
    fixture.detectChanges();

    expect(native.querySelector('.agent-window')).toBeTruthy();
  });

  it('toggle_shouldHideWindow_whenBubbleIsClickedAgain', () => {
    fixture.detectChanges();
    component.toggle();
    component.toggle();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.agent-window')).toBeNull();
  });

  it('close_shouldHideWindow_whenCloseButtonIsClicked', () => {
    fixture.detectChanges();
    component.toggle();
    fixture.detectChanges();

    component.close();
    fixture.detectChanges();

    expect(component.open()).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelector('.agent-window')).toBeNull();
  });

  it('ask_shouldSendTrimmedQuestionWithPosition_whenQuestionIsFilled', () => {
    component.question.set('  toilettes accessibles  ');
    component.ask();

    expect(agentMock.askAgent).toHaveBeenCalledWith({
      message: 'toilettes accessibles',
      latitude: 48.1119,
      longitude: -1.6745
    });
  });

  it('ask_shouldNotCallService_whenQuestionIsBlank', () => {
    component.question.set('   ');
    component.ask();

    expect(agentMock.askAgent).not.toHaveBeenCalled();
    expect(component.error()).toContain('Posez une question');
    expect(component.busy()).toBeFalse();
  });

  it('ask_shouldShowPendingTimeline_whileInferenceIsRunning', () => {
    const pending = new Subject<AgentResponse>();
    agentMock.askAgent.and.returnValue(pending);

    component.question.set('toilettes proches');
    component.ask();
    component.open.set(true);
    fixture.detectChanges();

    expect(component.busy()).toBeTrue();
    expect(component.canAsk()).toBeFalse();
    expect(component.timeline()).toEqual([
      'Analyse de la position…',
      'Recherche géospatiale des équipements…',
      'Interrogation du modèle local…'
    ]);

    const native = fixture.nativeElement as HTMLElement;
    expect(native.querySelectorAll('.agent-step').length).toBe(3);
    expect(native.querySelector('.agent-step--pending')).toBeTruthy();
  });

  it('ask_shouldReplaceTimelineWithAgentSteps_whenResponseArrives', () => {
    component.question.set('toilettes proches');
    component.ask();
    fixture.detectChanges();

    expect(component.busy()).toBeFalse();
    expect(component.timeline()).toEqual(chatResponse.steps);
    expect(component.reply()).toBe('Deux toilettes accessibles ont été trouvées.');
  });

  it('ask_shouldRenderMiniCards_whenAgentReturnsToilets', () => {
    component.question.set('toilettes proches');
    component.ask();
    component.open.set(true);
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;

    expect(component.resultCount()).toBe(2);
    expect(native.querySelectorAll('.agent-card').length).toBe(2);
    expect(native.querySelector('.agent-card__distance')?.textContent).toBe('120 m');
  });

  it('selectToilette_shouldEmitToilet_whenMiniCardIsClicked', () => {
    const emitted: any[] = [];
    component.selectToilette.subscribe(value => emitted.push(value));

    component.question.set('toilettes proches');
    component.ask();
    component.open.set(true);
    fixture.detectChanges();

    const firstCard = (fixture.nativeElement as HTMLElement).querySelector('.agent-card__button') as HTMLElement | null;
    firstCard?.click();

    expect(emitted).toEqual([toiletNear]);
  });

  it('askQuestion_shouldSendSuggestion_whenChipIsClicked', () => {
    component.askQuestion(component.suggestions[0]);

    expect(component.question()).toBe(component.suggestions[0]);
    expect(agentMock.askAgent).toHaveBeenCalled();
  });

  it('ask_shouldReportErrorMessage_whenInferenceServerIsUnreachable', () => {
    agentMock.askAgent.and.returnValue(throwError({ status: 503, message: 'Local inference server is unreachable' }));

    component.question.set('bonjour');
    component.ask();

    expect(component.error()).toBe('Local inference server is unreachable');
    expect(component.busy()).toBeFalse();
    expect(component.timeline()).toEqual([]);
  });

  it('ask_shouldExplainMissingRole_whenApiReturnsForbidden', () => {
    agentMock.askAgent.and.returnValue(throwError({ status: 403 }));

    component.question.set('supprime la toilette 3');
    component.ask();

    expect(component.error()).toBe('Le rôle ADMIN est requis pour cette opération.');
  });

  it('formatDistance_shouldSwitchToKilometers_whenDistanceExceedsOneThousand', () => {
    expect(component.formatDistance(toiletNear)).toBe('120 m');
    expect(component.formatDistance(toiletFar)).toBe('1.50 km');
    expect(component.formatDistance({ libelle: 'Sans distance' })).toBe('distance inconnue');
  });

  it('toiletLabel_shouldFallBack_whenLibelleIsMissing', () => {
    expect(component.toiletLabel(toiletNear)).toBe('WC de la Mairie');
    expect(component.toiletLabel({ distance: 10 })).toBe('Équipement sans nom');
    expect(component.toiletLabel(null)).toBe('Équipement sans nom');
  });
});

