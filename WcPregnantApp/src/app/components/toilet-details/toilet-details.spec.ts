import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ToiletDetailsComponent } from './toilet-details';
import { Toilet } from '../../models/toilet.model';

describe('ToiletDetailsComponent', () => {
  let fixture: ComponentFixture<ToiletDetailsComponent>;
  let component: ToiletDetailsComponent;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ToiletDetailsComponent]
    });

    fixture = TestBed.createComponent(ToiletDetailsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('getGoogleMapsLink_shouldBuildUrl_whenPositionIsSet', () => {
    component.toilet = {
      libelle: 'Toilettes du Parc',
      adress: { codePostal: '35000' },
      position: { latitude: 48.1119, longitude: -1.6745 }
    };

    expect(component.getGoogleMapsLink()).toBe(
      'https://www.google.com/maps/dir/?api=1&destination=48.1119,-1.6745'
    );
  });

  it('render_shouldShowNameAndDistance_whenToiletIsProvided', () => {
    const toilet: Toilet = {
      id: 3,
      libelle: 'Toilettes de la Gare',
      distance: 2.5,
      adress: { voie: 'Place de la Gare', codePostal: '35000', ville: 'Rennes' },
      position: { latitude: 48.1048, longitude: -1.1525 }
    };

    const fixture = TestBed.createComponent(ToiletDetailsComponent);
    fixture.componentInstance.toilet = toilet;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Toilettes de la Gare');
    expect(fixture.nativeElement.textContent).toContain('2.5');
  });
});
