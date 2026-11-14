/**
 * Adresse d'une toilette, alignée sur le DTO `Adress` du contrat OpenAPI
 * (orthographe "adress" conservée côté backend).
 */
export interface Adress {
  numero?: string;
  voie?: string;
  codePostal: string;
  ville?: string;
  pays?: string;
}

/** Notes qualitatives d'une toilette (0 à 10). */
export interface Graduation {
  proprete?: number;
  accueil?: number;
  espace?: number;
}

/** Position géographique WGS84. */
export interface GeoPosition {
  latitude: number;
  longitude: number;
}

/** Modèle d'une toilette publique exposé par l'API. */
export interface Toilet {
  id?: number;
  libelle: string;
  distance?: number;
  adress: Adress;
  grade?: Graduation;
  position: GeoPosition;
}
