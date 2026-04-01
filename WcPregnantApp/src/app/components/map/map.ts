import {
  Component,
  Input,
  Output,
  EventEmitter,
  AfterViewInit,
  OnDestroy
} from '@angular/core';
import * as L from 'leaflet';
import { Toilet } from '../../models/toilet.model';

@Component({
  selector: 'wc-map',
  templateUrl: './map.html',
  styleUrls: ['./map.css']
})
export class WcMapComponent implements AfterViewInit, OnDestroy {
  @Input() toilets: Toilet[] = [];
  @Output() doubleClick = new EventEmitter<{ lat: number; lng: number }>();
  @Output() toiletSelected = new EventEmitter<Toilet>();

  private map!: L.Map;
  private markers: L.Marker[] = [];

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    this.map = L.map('map').setView([48.11, -1.67], 14); // Cesson-Sévigné par défaut

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('dblclick', (e: L.LeafletMouseEvent) => {
      this.doubleClick.emit({ lat: e.latlng.lat, lng: e.latlng.lng });
    });

    this.updateMarkers();
  }

  private updateMarkers(): void {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    this.toilets.forEach(toilet => {
      const marker = L.marker([toilet.latitude, toilet.longitude])
        .addTo(this.map)
        .bindPopup(`<strong>${toilet.name}</strong><br>${toilet.address}`)
        .on('click', () => this.toiletSelected.emit(toilet));

      this.markers.push(marker);
    });
  }

  ngOnChanges(): void {
    if (this.map) {
      this.updateMarkers();
    }
  }
}
