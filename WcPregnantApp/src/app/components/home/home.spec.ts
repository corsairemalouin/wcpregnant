import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home';
import { ToiletService } from '../../services/toilet.service';
import { MapService } from '../../services/map.service';
import { SettingsService } from '../../services/settings.service';
import { Toilet } from '../../models/toilet.model';
import { environment } from '../../../environments/environment';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let component: HomeComponent;

  const STORAGE_KEY = 'wc-pregnant-developer-mode';

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

  // Géolocalisation simulée : le navigateur de test ne fournit pas de position réelle.
  const mapServiceMock = {
    getCurrentLocation: jasmine.createSpy('getCurrentLocation').and.resolveTo({ lat: 48.1234, lng: -1.6612 })
  };

  /** Laisse les micro-tâches (promesses) se résoudre avant les assertions. */
  const flush = () => new Promise<void>(resolve => setTimeout(resolve, 0));

  beforeEach(async () => {
    localStorage.removeItem(STORAGE_KEY);
    mapServiceMock.getCurrentLocation.and.resolveTo({ lat: 48.1234, lng: -1.6612 });

    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: HttpClient, useValue: httpMock },
        { provide: MapService, useValue: mapServiceMock }
      ]
    });

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('shouldCreate', () => {
    expect(component).toBeTruthy();
  });

  it('shouldStartOnGeolocation_whenComponentIsCreated', () => {
    expect(component.developerMode).toBeFalse();
    expect(component.searchPosition).toEqual({
      latitude: environment.defaultPosition.latitude,
      longitude: environment.defaultPosition.longitude
    });
  });

  it('login_shouldEnableSessionAndLoadNearestToilets_whenCredentialsProvided', async () => {
    httpMock.get.calls.reset();
    httpMock.get.and.returnValue(of([sampleToilet]));

    component.username = 'manu';
    component.password = 'manu';
    component.login();
    await flush();

    expect(component.loggedIn).toBeTrue();
    expect(component.toilets).toEqual([sampleToilet]);
    expect(httpMock.get.calls.argsFor(0)[0]).toContain('/toilets?latitude=');
  });

  it('login_shouldUseGeolocatedPosition_byDefault', async () => {
    httpMock.get.calls.reset();
    httpMock.get.and.returnValue(of([sampleToilet]));

    component.login();
    await flush();

    expect(component.positionSource).toBe('geolocation');
    expect(component.lat).toBe(48.1234);
    expect(component.lng).toBe(-1.6612);
    expect(component.searchPosition).toEqual({ latitude: 48.1234, longitude: -1.6612 });
    expect(httpMock.get.calls.argsFor(0)[0]).toContain('latitude=48.1234&longitude=-1.6612');
  });

  it('login_shouldFallBackToDefaultPosition_whenGeolocationFails', async () => {
    httpMock.get.calls.reset();
    httpMock.get.and.returnValue(of([]));
    mapServiceMock.getCurrentLocation.and.rejectWith('permission denied');

    component.login();
    await flush();

    expect(component.positionSource).toBe('default');
    expect(component.lat).toBe(environment.defaultPosition.latitude);
    expect(component.lng).toBe(environment.defaultPosition.longitude);
    expect(component.notice).toContain('Géolocalisation indisponible');
    expect(component.toilets).toEqual([]);
  });

  it('login_shouldSkipGeolocation_whenDeveloperModeIsEnabled', async () => {
    TestBed.inject(SettingsService).setDeveloperMode(true);
    httpMock.get.calls.reset();
    mapServiceMock.getCurrentLocation.calls.reset();
    httpMock.get.and.returnValue(of([sampleToilet]));

    component.username = 'manu';
    component.password = 'manu';
    component.login();
    await flush();

    expect(mapServiceMock.getCurrentLocation).not.toHaveBeenCalled();
    expect(component.developerMode).toBeTrue();
    expect(component.positionSource).toBe('default');
  });

  it('toggleDeveloperMode_shouldEnableManualCoordinates_whenCalled', () => {
    component.toggleDeveloperMode();

    expect(component.developerMode).toBeTrue();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
  });

  it('manualCoordinates_shouldBeHidden_whenDeveloperModeIsDisabled', () => {
    component.loggedIn = true;
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;

    expect(native.querySelector('#lat')).toBeNull();
    expect(native.querySelector('#lng')).toBeNull();
    expect(native.querySelector('.position-chip')).toBeTruthy();
  });

  it('manualCoordinates_shouldAppear_whenDeveloperModeIsEnabled', () => {
    component.loggedIn = true;
    component.toggleDeveloperMode();
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;

    expect(native.querySelector('#lat')).toBeTruthy();
    expect(native.querySelector('#lng')).toBeTruthy();
  });

  it('openSettings_shouldRenderSettingsPanel_whenClicked', () => {
    component.loggedIn = true;
    fixture.detectChanges();
    component.openSettings();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.settings-panel')).toBeTruthy();

    component.closeSettings();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.settings-panel')).toBeNull();
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

  it('loadAll_shouldListEveryToilet_whenDeveloperWantsTheFullList', () => {
    httpMock.get.calls.reset();
    httpMock.get.and.returnValue(of([sampleToilet]));

    component.loadAll();

    expect(component.toilets).toEqual([sampleToilet]);
    expect(httpMock.get.calls.argsFor(0)[0]).toContain('/toilets?page=0&size=20');
  });

  it('useMyLocation_shouldReportNotice_whenBrowserRefusesGeolocation', async () => {
    mapServiceMock.getCurrentLocation.and.rejectWith('Geolocation is not supported by this browser.');
    httpMock.get.and.returnValue(of([]));

    component.useMyLocation();
    await flush();

    expect(component.notice).toContain('Géolocalisation indisponible');
    expect(component.positionSource).toBe('default');
  });

  it('settingsService_shouldBeSharedWithComponent', () => {
    const settings = TestBed.inject(SettingsService);

    component.toggleDeveloperMode();

    expect(settings.isDeveloperMode).toBeTrue();
  });

  it('agentChat_shouldBeEmbedded_whenUserIsLoggedIn', () => {
    component.loggedIn = true;
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('wc-agent-chat')).toBeTruthy();
  });

  it('agentFab_shouldBeEmbedded_whenUserIsLoggedIn', () => {
    component.loggedIn = true;
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('wc-agent-chat-fab')).toBeTruthy();
  });

  it('onAgentResults_shouldReplaceListAndHighlightMap_whenAgentFindsToilets', () => {
    component.toilets = [];
    component.onAgentResults([sampleToilet]);

    expect(component.toilets).toEqual([sampleToilet]);
    expect(component.agentHighlight).toEqual(sampleToilet);
    expect(component.notice).toContain('L\'agent a trouvé 1 équipement(s)');
  });

  it('onAgentResults_shouldKeepExistingList_whenAgentFindsNothing', () => {
    component.toilets = [sampleToilet];
    component.onAgentResults([]);

    expect(component.toilets).toEqual([sampleToilet]);
    expect(component.notice).toContain('aucun équipement');
  });

  it('selectFromAgent_shouldOpenDetailsAndHighlightToilet_whenAgentCardIsClicked', () => {
    component.selectFromAgent(sampleToilet);

    expect(component.selectedToilet).toEqual(sampleToilet);
    expect(component.agentHighlight).toEqual(sampleToilet);
  });

  it('logout_shouldForgetAgentSelection_whenSessionEnds', () => {
    component.login();
    component.onAgentResults([sampleToilet]);
    component.logout();

    expect(component.agentHighlight).toBeNull();
  });
});
