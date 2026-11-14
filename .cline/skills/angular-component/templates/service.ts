import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Toilet } from '../models/toilet.model';

@Injectable({ providedIn: 'root' })
export class ToiletService {
  // Base URL injectée par l'environnement, jamais codée en dur.
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  getNearbyToilets(lat: number, lng: number): Observable<Toilet[]> {
    return this.http.get<Toilet[]>(`${this.baseUrl}/toilets`, {
      params: { latitude: lat, longitude: lng },
    });
  }

  addToilet(toilet: Toilet): Observable<Toilet> {
    return this.http.post<Toilet>(`${this.baseUrl}/toilets`, toilet);
  }
}
