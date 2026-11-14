import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToiletService } from '../../services/toilet.service';
import { AuthService } from '../../services/auth.service';
import { MapService } from '../../services/map.service';
import { SettingsService } from '../../services/settings.service';
import { Toilet, GeoPosition } from '../../models/toilet.model';
import { environment } from '../../../environments/environment';
import { WcMapComponent } from '../map/map';
import { ToiletFormComponent } from '../toilet-form/toilet-form';
import { ToiletDetailsComponent } from '../toilet-details/toilet-details';

/** Origine du point de recherche : géolocalisation ou position de référence. */
export type PositionSource = 'geolocation' | 'default';

/**
 * Page principale : authentification, recherche de proximité,
 * liste des toilettes, carte Leaflet et formulaire d'ajout.
 *
 * Par défaut la recherche part de la position géolocalisée de l'utilisateur.
 * Les champs latitude/longitude ne sont exposés qu'en "mode développeur",
 * activable depuis le menu Paramètres.
 */
@Component({
  selector: 'wc-home',
  standalone: true,
  imports: [FormsModule, WcMapComponent, ToiletFormComponent, ToiletDetailsComponent],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class HomeComponent {
  username = 'manu';
  password = 'manu';
  lat = environment.defaultPosition.latitude;
  lng = environment.defaultPosition.longitude;
  loggedIn = false;
  toilets: Toilet[] = [];
  selectedToilet: Toilet | null = null;
  error: string | null = null;
  notice: string | null = null;
  settingsOpen = false;
  positionSource: PositionSource = 'default';
  searchPosition: GeoPosition = { ...environment.defaultPosition };

  constructor(
    private toiletService: ToiletService,
    private auth: AuthService,
    private mapService: MapService,
    private settings: SettingsService
  ) {}

  /** Le mode développeur expose la saisie manuelle des coordonnées. */
  get developerMode(): boolean {
    return this.settings.isDeveloperMode;
  }

  /**
   * Authentifie l'utilisateur puis lance la recherche : géolocalisation
   * par défaut, position de référence en mode développeur.
   */
  login(): void {
    this.auth.login(this.username, this.password);
    this.loggedIn = true;
    this.startSearch();
  }

  /**
   * Ferme la session et vide la liste affichée.
   */
  logout(): void {
    this.auth.logout();
    this.loggedIn = false;
    this.toilets = [];
    this.selectedToilet = null;
    this.error = null;
    this.notice = null;
  }

  /**
   * Ouvre le menu Paramètres.
   */
  openSettings(): void {
    this.settingsOpen = true;
  }

  /**
   * Ferme le menu Paramètres.
   */
  closeSettings(): void {
    this.settingsOpen = false;
  }

  /**
   * Bascule le mode développeur (saisie manuelle latitude/longitude).
   *
   * @returns la valeur du mode après la bascule
   */
  toggleDeveloperMode(): boolean {
    return this.settings.toggleDeveloperMode();
  }

  /**
   * Charge les toilettes les plus proches de la position de recherche.
   */
  loadNearest(): void {
    this.error = null;
    this.searchPosition = { latitude: this.lat, longitude: this.lng };
    this.toiletService.findNearest(this.lat, this.lng).subscribe({
      next: toilets => this.toilets = toilets,
      error: err => this.error = err.message ?? 'Unable to load toilets'
    });
  }

  /**
   * Charge la liste complète des toilettes enregistrées.
   */
  loadAll(): void {
    this.error = null;
    this.toiletService.getToilets().subscribe({
      next: toilets => this.toilets = toilets,
      error: err => this.error = err.message ?? 'Unable to load toilets'
    });
  }

  /**
   * Remplace la position de recherche par la géolocalisation du navigateur.
   * En cas d'échec, la recherche retombe sur la position de référence.
   */
  useMyLocation(): void {
    this.notice = null;
    this.mapService.getCurrentLocation().then(position => {
      this.lat = position.lat;
      this.lng = position.lng;
      this.positionSource = 'geolocation';
      this.loadNearest();
    }).catch(err => {
      this.lat = environment.defaultPosition.latitude;
      this.lng = environment.defaultPosition.longitude;
      this.positionSource = 'default';
      this.notice = `Géolocalisation indisponible : ${String(err)}. Recherche sur la position de référence.`;
      this.loadNearest();
    });
  }

  /**
   * Sélectionne une toilette pour l'afficher dans le panneau de détails.
   *
   * @param toilet toilette sélectionnée
   */
  select(toilet: Toilet): void {
    this.selectedToilet = toilet;
  }

  /**
   * Ajoute une toilette créée depuis le formulaire à la liste.
   *
   * @param toilet toilette retournée par l'API
   */
  onCreated(toilet: Toilet): void {
    this.toilets = [toilet, ...this.toilets];
    this.selectedToilet = toilet;
  }

  /**
   * Supprime une toilette (opération réservée au rôle ADMIN).
   *
   * @param toilet toilette à supprimer
   */
  remove(toilet: Toilet): void {
    if (toilet.id === undefined) {
      return;
    }
    this.toiletService.deleteToilet(toilet.id).subscribe({
      next: () => {
        this.toilets = this.toilets.filter(t => t.id !== toilet.id);
        if (this.selectedToilet?.id === toilet.id) {
          this.selectedToilet = null;
        }
      },
      error: err => this.error = err.message ?? 'Unable to delete toilet'
    });
  }

  /**
   * Point de départ de la recherche après la connexion.
   */
  private startSearch(): void {
    if (this.developerMode) {
      this.positionSource = 'default';
      this.loadNearest();
      return;
    }
    this.useMyLocation();
  }
}
