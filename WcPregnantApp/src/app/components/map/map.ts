import { Component, Input, SimpleChange } from '@angular/core';
import * as L from 'leaflet';
import { Toilet } from '../../models/toilet.model';

/**
 * Carte Leaflet affichant les toilettes référencées.
 * L'instance est détruite à la fermeture du composant pour éviter les fuites.
 */
@Component({
  selector: 'wc-map',
  standalone: true,
  templateUrl: './map.html',
  styleUrl: './map.css'
})
export class WcMapComponent {
  @Input() toilets: Toilet[] = [];

  private map: L.Map | null = null;
  private markers: L.Marker[] = [];

  ngAfterViewInit(): void {
    this.map = L.map('map').setZoom(13);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);

    this.renderMarkers();
  }

  ngOnChanges(changes: Record<string, SimpleChange>): void {
    if (this.map && changes['toilets']) {
      this.renderMarkers();
    }
  }

  ngOnDestroy(): void {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];
    this.map?.remove();
    this.map = null;
  }

  /**
   * (Re)dessine un marqueur pour chaque toilette et recentre la carte.
   */
  private renderMarkers(): void {
    if (!this.map) {
      return;
    }

    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    this.toilets.forEach(toilet => {
      const marker = L.marker([toilet.position.latitude, toilet.position.longitude]).addTo(this.map!);
      marker.bindTooltip(toilet.libelle);
      marker.bindTooltip(this.tooltip(toilet));
      this.markers.push(marker);
    });

    if (this.toilets.length > 0) {
      this.map.panTo([this.toilets[0].position.latitude, this.toilets[0].position.longitude]);
    }
  }

  /**
   * Construit le texte du tooltip d'un marqueur.
   *
   * @param toilet toilette affichée
   * @returns contenu HTML du tooltip
   */
  private tooltip(toilet: Toilet): string {
    const distance = toilet.distance === undefined ? '' : ` — ${toilet.distance} km`;
    return `<strong>${toilet.libelle}</strong>${distance}`;
  }
}
