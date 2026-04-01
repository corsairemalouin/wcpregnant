import { Component, OnInit } from '@angular/core';
import { ToiletService } from '../../services/toilet.service';
import { MapService } from '../../services/map.service';
import { Toilet } from '../../models/toilet.model';
import { WcMapComponent } from "../map/map";
import { ToiletFormComponent } from "../toilet-form/toilet-form";
import { ToiletDetailsComponent } from "../toilet-details/toilet-details";

import { NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'wc-home',
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  toilets: Toilet[] = [];
  selectedToilet?: Toilet;
  showMap = false;
  showForm = false;
  prefilledLocation: { lat: number; lng: number } | null = null;

  constructor(
    private toiletService: ToiletService,
    private mapService: MapService
  ) {}

  ngOnInit(): void {
    this.mapService.getCurrentLocation().then(location => {
      this.toiletService.getNearbyToilets(location).subscribe(data => {
        this.toilets = data;
      });
    });
  }

  onShowMap(): void {
    this.showMap = true;
  }

  onRegisterToilet(): void {
    this.mapService.getCurrentLocation().then(location => {
      this.prefilledLocation = location;
      this.showForm = true;
    });
  }

  onMapDoubleClick(location: { lat: number; lng: number }): void {
    this.prefilledLocation = location;
    this.showForm = true;
  }

  onToiletSelected(toilet: Toilet): void {
    this.selectedToilet = toilet;
  }
}
