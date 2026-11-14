import { Component, Input } from '@angular/core';
import { Toilet } from '../../models/toilet.model';

/**
 * Panneau de détails d'une toilette : adresse, distance et notes qualitatives.
 */
@Component({
  selector: 'wc-toilet-details',
  standalone: true,
  imports: [],
  templateUrl: './toilet-details.html',
  styleUrls: ['./toilet-details.css']
})
export class ToiletDetailsComponent {
  @Input() toilet!: Toilet;

  /**
   * Lien Google Maps permettant de rejoindre la toilette.
   *
   * @returns URL d'itinéraire vers la position de la toilette
   */
  getGoogleMapsLink(): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${this.toilet.position.latitude},${this.toilet.position.longitude}`;
  }
}
