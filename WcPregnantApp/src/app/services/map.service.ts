import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class MapService {
  constructor() {}

  /**
   * Récupère la position géographique actuelle de l'utilisateur.
   */
  getCurrentLocation(): Promise<{ lat: number; lng: number }> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject('La géolocalisation n’est pas supportée par ce navigateur.');
      } else {
        navigator.geolocation.getCurrentPosition(
          position => {
            resolve({
              lat: position.coords.latitude,
              lng: position.coords.longitude
            });
          },
          error => {
            reject('Impossible de récupérer la position : ' + error.message);
          }
        );
      }
    });
  }
}
