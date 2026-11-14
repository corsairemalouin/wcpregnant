import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { ToiletService } from './toilet.service';
import { AuthService } from './auth.service';
import { Toilet } from '../models/toilet.model';

describe('ToiletService', () => {
  let service: ToiletService;
  let auth: AuthService;
  const httpMock = {
    get: jasmine.createSpy('get').and.returnValue(of([])),
    post: jasmine.createSpy('post').and.returnValue(of({})),
    put: jasmine.createSpy('put').and.returnValue(of({})),
    delete: jasmine.createSpy('delete').and.returnValue(of(undefined))
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: HttpClient, useValue: httpMock }
      ]
    });
    service = TestBed.inject(ToiletService);
    auth = TestBed.inject(AuthService);
    httpMock.get.calls.reset();
    httpMock.post.calls.reset();
    httpMock.delete.calls.reset();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getToilets_shouldRequestPagination_whenCalled', () => {
    service.getToilets(1, 5).subscribe();

    expect(httpMock.get).toHaveBeenCalledTimes(1);
    expect(httpMock.get.calls.argsFor(0)[0]).toContain('/toilets?page=1&size=5');
  });

  it('findNearest_shouldPassCoordinates_whenCalled', () => {
    service.findNearest(48.1119, -1.6745).subscribe();

    expect(httpMock.get.calls.argsFor(0)[0]).toContain('/toilets?latitude=48.1119&longitude=-1.6745');
  });

  it('requests_shouldCarryAuthorizationHeader_whenUserIsAuthenticated', () => {
    auth.login('manu', 'manu');

    service.getToilets().subscribe();

    expect(httpMock.get.calls.argsFor(0)[1].headers.Authorization).toBe(`Basic ${btoa('manu:manu')}`);
  });

  it('requests_shouldHaveNoAuthorizationHeader_whenAnonymous', () => {
    service.getToilets().subscribe();

    expect(httpMock.get.calls.argsFor(0)[1]).toEqual({});
  });

  it('createToilet_shouldPostPayload_whenCalled', () => {
    const toilet: Toilet = {
      libelle: 'Toilettes du Parc',
      adress: { codePostal: '35000', ville: 'Rennes' },
      position: { latitude: 48.11, longitude: -1.67 }
    };

    service.createToilet(toilet).subscribe();

    expect(httpMock.post).toHaveBeenCalledTimes(1);
    expect(httpMock.post.calls.argsFor(0)[1]).toEqual(toilet);
  });

  it('deleteToilet_shouldCallDeleteEndpoint_whenIdProvided', () => {
    service.deleteToilet(42).subscribe();

    expect(httpMock.delete).toHaveBeenCalledTimes(1);
    expect(httpMock.delete.calls.argsFor(0)[0]).toContain('/toilets/42');
  });
});
