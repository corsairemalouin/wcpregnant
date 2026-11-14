import { Component, EventEmitter, Input, Output, SimpleChange } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Toilet, GeoPosition } from '../../models/toilet.model';
import { environment } from '../../../environments/environment';

/**
 * Formulaire de création d'une toilette, conforme au DTO `Toilet`
 * du contrat OpenAPI (libelle, adress et position obligatoires).
 */
@Component({
  selector: 'wc-toilet-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './toilet-form.html',
  styleUrl: './toilet-form.css'
})
export class ToiletFormComponent {
  /** Position de recherche utilisée pour pré-remplir les coordonnées. */
  @Input() location: GeoPosition | null = null;

  /** Émet le payload de création lorsque le formulaire est valide. */
  @Output() created = new EventEmitter<Toilet>();

  readonly form: FormGroup;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      libelle: ['', [Validators.required, Validators.maxLength(100)]],
      numero: [''],
      voie: [''],
      codePostal: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
      ville: [''],
      pays: ['France'],
      latitude: [environment.defaultPosition.latitude, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitude: [environment.defaultPosition.longitude, [Validators.required, Validators.min(-180), Validators.max(180)]],
      proprete: [5, [Validators.min(0), Validators.max(10)]],
      accueil: [5, [Validators.min(0), Validators.max(10)]],
      espace: [5, [Validators.min(0), Validators.max(10)]]
    });
  }

  /**
   * Pré-remplit les coordonnées du formulaire avec la position de recherche
   * (géolocalisation par défaut).
   */
  ngOnChanges(changes: Record<string, SimpleChange>): void {
    if (changes['location'] && this.location) {
      this.form.patchValue({
        latitude: this.location.latitude,
        longitude: this.location.longitude
      });
    }
  }

  /**
   * Valide le formulaire et émet le payload de création si tout est correct.
   * Un formulaire invalide est silencieusement ignoré : les messages de
   * validation s'affichent dans les champs.
   */
  submit(): void {
    if (this.form.invalid) {
      return;
    }

    const value = this.form.value;

    this.created.emit({
      libelle: value.libelle as string,
      adress: {
        numero: value.numero,
        voie: value.voie,
        codePostal: value.codePostal as string,
        ville: value.ville,
        pays: value.pays
      },
      grade: {
        proprete: value.proprete,
        accueil: value.accueil,
        espace: value.espace
      },
      position: {
        latitude: value.latitude as number,
        longitude: value.longitude as number
      }
    });
  }
}
