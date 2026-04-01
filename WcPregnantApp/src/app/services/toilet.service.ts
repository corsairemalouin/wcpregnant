import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Toilet } from '../models/toilet.model';

@Injectable({
  providedIn: 'root'
})
export class ToiletService {
  private apiUrl = 'http://localhost:8080/api/'; // à adapter selon ton backend

  constructor(private http: HttpClient) {}

  getNearbyToilets(location: { lat: number; lng: number }): Observable<Toilet[]> {
    return this.http.get<Toilet[]>(`${this.apiUrl}/nearby?lat=${location.lat}&lng=${location.lng}`);
  }

  getToiletById(id: number): Observable<Toilet> {
    return this.http.get<Toilet>(`${this.apiUrl}/${id}`);
  }

  addToilet(toilet: Toilet): Observable<Toilet> {
    return this.http.post<Toilet>(this.apiUrl, toilet);
  }

  rateToilet(id: number, rating: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/rate`, { rating });
  }

  uploadPhotos(id: number, files: File[]): Observable<any> {
    const formData = new FormData();
    files.forEach(file => formData.append('photos', file));
    return this.http.post(`${this.apiUrl}/${id}/photos`, formData);
  }
}
