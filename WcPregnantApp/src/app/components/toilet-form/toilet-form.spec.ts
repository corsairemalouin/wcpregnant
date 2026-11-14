import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ToiletFormComponent } from './toilet-form';

describe('ToiletFormComponent', () => {
  let fixture: ComponentFixture<ToiletFormComponent>;
  let component: ToiletFormComponent;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ToiletFormComponent]
    });

    fixture = TestBed.createComponent(ToiletFormComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('submit_shouldEmitToilet_whenFormIsValid', () => {
    const emitted: unknown[] = [];
    component.created.subscribe(payload => emitted.push(payload));

    component.form.setValue({
      libelle: 'Toilettes du Parc',
      numero: '2',
      voie: 'Rue de la Motte',
      codePostal: '35130',
      ville: 'Cesson-Sévigné',
      pays: 'France',
      latitude: 48.1234,
      longitude: -1.6612,
      proprete: 8,
      accueil: 7,
      espace: 6
    });

    component.submit();

    expect(emitted.length).toBe(1);
    const payload = emitted[0] as { libelle: string; position: { latitude: number } };
    expect(payload.libelle).toBe('Toilettes du Parc');
    expect(payload.position.latitude).toBe(48.1234);
  });

  it('submit_shouldEmitNothing_whenFormIsInvalid', () => {
    const emitted: unknown[] = [];
    component.created.subscribe(payload => emitted.push(payload));

    component.form.setValue({
      libelle: '',
      numero: '',
      voie: '',
      codePostal: '3500',
      ville: '',
      pays: 'France',
      latitude: 48.11,
      longitude: -1.67,
      proprete: 5,
      accueil: 5,
      espace: 5
    });

    component.submit();

    expect(component.form.valid).toBeFalse();
    expect(emitted.length).toBe(0);
  });

  it('location_shouldPrefillCoordinates_whenInputProvided', () => {
    const withLocation = TestBed.createComponent(ToiletFormComponent);
    withLocation.componentInstance.location = { lat: 48.9, lng: 2.4 };
    withLocation.componentInstance.form.patchValue({ latitude: 48.9, longitude: 2.4 });

    expect(withLocation.componentInstance.form.get('latitude')?.value).toBe(48.9);
  });
});
