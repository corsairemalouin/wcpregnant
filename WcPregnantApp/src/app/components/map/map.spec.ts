import { TestBed, ComponentFixture } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { WcMapComponent } from './map';
import { Toilet } from '../../models/toilet.model';

describe('WcMapComponent', () => {
  let fixture: ComponentFixture<WcMapComponent>;
  let component: WcMapComponent;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [WcMapComponent]
    });

    fixture = TestBed.createComponent(WcMapComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('toilets_shouldDefaultToEmptyList_whenNotProvided', () => {
    expect(component.toilets).toEqual([]);
  });

  it('toilets_shouldBeStored_whenInputIsSet', () => {
    const toilet: Toilet = {
      libelle: 'Toilettes du Parc',
      adress: { codePostal: '35000' },
      position: { latitude: 48.11, longitude: -1.67 }
    };

    component.toilets = [toilet];

    expect(component.toilets.length).toBe(1);
  });

  it('origin_shouldBeNull_whenNotProvided', () => {
    expect(component.origin).toBeNull();
  });

  it('origin_shouldBeStored_whenSearchPositionIsSet', () => {
    component.origin = { latitude: 48.1119, longitude: -1.6745 };

    expect(component.origin).toEqual({ latitude: 48.1119, longitude: -1.6745 });
  });

  it('ngOnChanges_shouldNotThrow_whenMapIsNotInitializedYet', () => {
    component.toilets = [];
    component.origin = { latitude: 48.1119, longitude: -1.6745 };

    component.ngOnChanges({
      toilets: new SimpleChange(null, component.toilets, true),
      origin: new SimpleChange(null, component.origin, true)
    });

    expect(component.origin).toEqual({ latitude: 48.1119, longitude: -1.6745 });
  });
});
