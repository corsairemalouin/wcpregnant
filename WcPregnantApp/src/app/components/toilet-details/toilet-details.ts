import { Component, Input } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { Toilet } from '../../models/toilet.model';

@Component({
  selector: 'wc-toilet-details',
  templateUrl: './toilet-details.html',
  styleUrls: ['./toilet-details.css'],
  imports: [NgIf, NgFor] // or CommonModule

})
export class ToiletDetailsComponent {
  @Input() toilet!: Toilet;

  getGoogleMapsLink(): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${this.toilet.latitude},${this.toilet.longitude}`;
  }
}
