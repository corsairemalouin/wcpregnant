import { Toilet, Adress, Graduation, GeoPosition } from './toilet.model';

describe('Toilet model', () => {
  it('shouldAcceptMinimalPayload_whenOnlyRequiredFieldsAreSet', () => {
    const toilet: Toilet = {
      libelle: 'Toilettes du Parc',
      adress: { codePostal: '35000' },
      position: { latitude: 48.1119, longitude: -1.6745 }
    };

    expect(toilet.libelle).toBe('Toilettes du Parc');
    expect(toilet.id).toBeUndefined();
    expect(toilet.distance).toBeUndefined();
  });

  it('shouldAcceptOptionalFields_whenGradeAndDistanceAreProvided', () => {
    const adress: Adress = { numero: '2', voie: 'Rue de la Motte', codePostal: '35130', ville: 'Cesson-Sévigné', pays: 'France' };
    const grade: Graduation = { proprete: 8, accueil: 7, espace: 6 };
    const position: GeoPosition = { latitude: 48.1234, longitude: -1.6612 };

    const toilet: Toilet = { id: 1, libelle: 'Toilettes du Parc', distance: 1.2, adress, grade, position };

    expect(toilet.id).toBe(1);
    expect(toilet.distance).toBeCloseTo(1.2);
    expect(toilet.grade?.proprete).toBe(8);
  });

  it('shouldKeepBackendSpelling_whenAddressIsUsed', () => {
    const adress: Adress = { codePostal: '35000', voie: 'Rue de Rennes' };

    expect(adress.voie).toBe('Rue de Rennes');
  });
});
