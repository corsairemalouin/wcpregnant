import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToiletService } from '../../services/toilet.service';
import { AuthService } from '../../services/auth.service';
import { MapService } from '../../services/map.service';
import { Toilet } from '../../models/toilet.model';
import { environment } from '../../../environments/environment';
import { WcMapComponent } from '../map/map';
import { ToiletFormComponent } from '../toilet-form/toilet-form';
import { ToiletDetailsComponent } from '../toilet-details/toilet-details';

/**
 * Page principale : authentification, recherche de proximité,
 * liste des toilettes, carte Leaflet et formulaire d'ajout.
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

  constructor(
    private toiletService: ToiletService,
    private auth: AuthService,
    private mapService: MapService
  ) {}

  /**
   * Authentifie l'utilisateur et charge les toilettes proches.
   */
  login(): void {
    this.auth.login(this.username, this.password);
    this.loggedIn = true;
    this.loadNearest();
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
  }

  /**
   * Charge les toilettes les plus proches de la position saisie.
   */
  loadNearest(): void {
    this.error = null;
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
   */
  useMyLocation(): void {
    this.mapService.getCurrentLocation().then(position => {
      this.lat = position.lat;
      this.lng = position.lng;
      this.loadNearest();
    }).catch(err => this.error = String(err));
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
}
