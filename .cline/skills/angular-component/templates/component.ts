import { Component } from '@angular/core';
import { Toilet } from '../models/toilet.model';

@Component({
  selector: 'app-toilet-list',
  standalone: true,
  template: `
    <section class="toilet-list">
      <h1>Toilettes à proximité</h1>
      <ul>
        @for (toilet of toilets; let toilet; let index; track toilet.id) {
          <li>
            <strong>{{ toilet.libelle }}</strong>
            <span>{{ toilet.distance }} m</span>
          </li>
        }
      </ul>
      @if (toilets.length === 0) {
        <p>Aucune toilette trouvée.</p>
      }
    </section>
  `,
})
export class ToiletListComponent {
  toilets: Toilet[] = [];
}
