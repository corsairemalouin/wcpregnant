import { Component, Input, SimpleChange } from '@angular/core';
import * as L from 'leaflet';
import { Toilet, GeoPosition } from '../../models/toilet.model';

/**
 * Carte Leaflet affichant les toilettes référencées.
 * L'instance est détruite à la fermeture du composant pour éviter les fuites.
 */
@Component({
  selector: 'wc-map',
  standalone: true,
  imports: [],
  templateUrl: './map.html',
  styleUrl: './map.css'
})
export class WcMapComponent {
  @Input() toilets: Toilet[] = [];

  /**
   * Position de recherche (géolocalisation par défaut) matérialisée
   * par un marqueur distinct sur la carte.
   */
  @Input() origin: GeoPosition | null = null;

  /**
   * Équipement mis en avant par l'assistant agentique : il est dessiné
   * avec un marqueur distinctif et la carte se centre dessus.
   */
  @Input() highlight: Toilet | null = null;

  private map: L.Map | null = null;
  private markers: L.Marker[] = [];
  private originMarker: L.CircleMarker | null = null;
  private highlightMarker: L.CircleMarker | null = null;

  ngAfterViewInit(): void {
    this.map = L.map('map').setZoom(13);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);

    this.renderMarkers();
  }

  ngOnChanges(changes: Record<string, SimpleChange>): void {
    if (!this.map) {
      return;
    }

    if (changes['toilets'] || changes['origin']) {
      this.renderMarkers();
    }

    if (changes['highlight']) {
      this.renderHighlight();
    }
  }

  ngOnDestroy(): void {
    this.markers.forEach(marker => marker.remove());
    this.originMarker?.remove();
    this.highlightMarker?.remove();
    this.markers = [];
    this.originMarker = null;
    this.highlightMarker = null;
    this.map?.remove();
    this.map = null;
  }

  /**
   * (Re)dessine un marqueur pour chaque toilette, le point de recherche,
   * et recentre la carte sur ce point.
   */
  private renderMarkers(): void {
    if (!this.map) {
      return;
    }

    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    this.toilets.forEach(toilet => {
      const marker = L.marker([toilet.position.latitude, toilet.position.longitude]).addTo(this.map!);
      marker.bindTooltip(this.tooltip(toilet));
      this.markers.push(marker);
    });

    this.renderOrigin();
    this.renderHighlight();

    const target = this.highlight?.position
      ?? this.origin
      ?? (this.toilets.length > 0 ? this.toilets[0].position : null);

    if (target) {
      this.map.panTo([target.latitude, target.longitude]);
    }
  }

  /**
   * Dessine le marqueur du point de recherche, s'il est fourni.
   */
  private renderOrigin(): void {
    if (!this.map) {
      return;
    }

    this.originMarker?.remove();
    this.originMarker = null;

    if (!this.origin) {
      return;
    }

    this.originMarker = L.circleMarker([this.origin.latitude, this.origin.longitude], {
      radius: 9,
      color: '#f06292',
      fillColor: '#f48fb1',
      fillOpacity: 0.35
    }).addTo(this.map);
    this.originMarker.bindTooltip('Position de recherche');
  }

  /**
   * Dessine le marqueur de l'équipement mis en avant par l'agent, s'il existe.
   */
  private renderHighlight(): void {
    if (!this.map) {
      return;
    }

    this.highlightMarker?.remove();
    this.highlightMarker = null;

    if (!this.highlight) {
      return;
    }

    this.highlightMarker = L.circleMarker([this.highlight.position.latitude, this.highlight.position.longitude], {
      radius: 12,
      color: '#b00020',
      fillColor: '#f48fb1',
      fillOpacity: 0.45,
      className: 'agent-highlight'
    }).addTo(this.map);
    this.highlightMarker.bindTooltip(`Sélection de l'agent : ${this.highlight.libelle}`);
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
