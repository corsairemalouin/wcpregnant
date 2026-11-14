import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home';
import { ToiletService } from '../../services/toilet.service';
import { Toilet } from '../../models/toilet.model';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let component: HomeComponent;

  const sampleToilet: Toilet = {
    id: 1,
    libelle: 'Toilettes du Parc',
    distance: 1.2,
    adress: { voie: 'Rue de la Motte', codePostal: '35130', ville: 'Cesson-Sévigné' },
    position: { latitude: 48.1234, longitude: -1.6612 }
  };

  const httpMock = {
    get: jasmine.createSpy('get').and.returnValue(of([])),
    post: jasmine.createSpy('post').and.returnValue(of({})),
    delete: jasmine.createSpy('delete').and.returnValue(of(undefined))
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [{ provide: HttpClient, useValue: httpMock }]
    });

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('login_shouldEnableSessionAndLoadNearestToilets_whenCredentialsProvided', () => {
    httpMock.get.and.returnValue(of([sampleToilet]));

    component.username = 'manu';
    component.password = 'manu';
    component.login();

    expect(component.loggedIn).toBeTrue();
    expect(component.toilets).toEqual([sampleToilet]);
    expect(httpMock.get.calls.argsFor(0)[0]).toContain('/toilets?latitude=');
  });

  it('logout_shouldClearSession_whenCalled', () => {
    component.login();
    component.logout();

    expect(component.loggedIn).toBeFalse();
    expect(component.toilets).toEqual([]);
    expect(component.selectedToilet).toBeNull();
  });

  it('remove_shouldDropToiletFromList_whenApiSucceeds', () => {
    component.toilets = [sampleToilet];
    component.selectedToilet = sampleToilet;

    component.remove(sampleToilet);

    expect(component.toilets).toEqual([]);
    expect(component.selectedToilet).toBeNull();
  });

  it('loadNearest_shouldReportError_whenApiFails', () => {
    const service = TestBed.inject(ToiletService);
    spyOn(service, 'findNearest').and.returnValue(throwError({ message: 'boom' }));

    component.loadNearest();

    expect(component.error).toBe('boom');
    expect(component.toilets).toEqual([]);
  });
});
