import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { AgentService } from './agent.service';
import { AuthService } from './auth.service';
import { AgentChatRequest, AgentChatResponse } from '../models/agent.model';
import { environment } from '../../environments/environment';

describe('AgentService', () => {
  let service: AgentService;
  let httpMock: { post: jasmine.Spy; get: jasmine.Spy };
  let auth: AuthService;

  const response: AgentChatResponse = {
    answer: 'La plus proche est à 120 m.',
    model: 'Qwen-3.8-Flash-Next',
    iterations: 2,
    elapsedMillis: 420,
    toolCalls: [{ tool: 'search_toilettes_proches', status: 'OK', result: { count: 1 } }]
  };

  beforeEach(() => {
    httpMock = {
      post: jasmine.createSpy('post').and.returnValue(of(response)),
      get: jasmine.createSpy('get').and.returnValue(of([]))
    };
    auth = new AuthService();

    TestBed.configureTestingModule({
      providers: [
        { provide: HttpClient, useValue: httpMock },
        { provide: AuthService, useValue: auth }
      ]
    });

    service = TestBed.inject(AgentService);
  });

  it('chat_shouldPostToAgentEndpoint_whenSessionIsOpen', () => {
    auth.login('manu', 'manu');
    const request: AgentChatRequest = {
      message: 'toilettes proches',
      position: { latitude: 48.1119, longitude: -1.6745 }
    };

    service.chat(request).subscribe();

    expect(httpMock.post).toHaveBeenCalledWith(
      `${environment.agentApiUrl}/chat`,
      request,
      { headers: { Authorization: auth.header } }
    );
  });

  it('chat_shouldSendNoHeader_whenUserIsNotAuthenticated', () => {
    service.chat({ message: 'hello' }).subscribe();

    expect(httpMock.post).toHaveBeenCalledWith(`${environment.agentApiUrl}/chat`, { message: 'hello' }, { headers: {} });
  });

  it('getTools_shouldReadTheToolRegistry_whenCalled', () => {
    service.getTools().subscribe();

    expect(httpMock.get).toHaveBeenCalledWith(`${environment.agentApiUrl}/tools`, { headers: {} });
  });

  it('chat_shouldReturnServerAnswer_whenObservableResolves', () => {
    const result: AgentChatResponse = { answer: 'ok' };
    httpMock.post.and.returnValue(of(result));

    let emitted: AgentChatResponse | undefined;
    service.chat({ message: 'ok' }).subscribe(value => emitted = value);

    expect(emitted).toEqual(result);
  });
});
