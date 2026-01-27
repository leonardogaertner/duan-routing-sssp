import { Component, OnInit } from '@angular/core';
// Removido RouterOutlet daqui
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { RouteService } from './route.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule], // Removido RouterOutlet daqui também
  templateUrl: './app.html',
})
export class AppComponent implements OnInit { // <--- Veja que o nome da classe é AppComponent
  private map!: L.Map;
  info: any = null;

  constructor(private routeService: RouteService) {}

  ngOnInit() {
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map('map').setView([41.085, -73.54], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);
  }

  loadRoute() {
    this.routeService.getRoute(1, 500).subscribe({
      next: (data) => {
        const latLngs: L.LatLngExpression[] = data.path.map(n => [n.lat, n.lon]);
        
        this.map.eachLayer((layer: any) => {
          if (layer instanceof L.Polyline) { layer.remove(); }
        });

        const line = L.polyline(latLngs, { color: 'red', weight: 6 }).addTo(this.map);
        this.map.fitBounds(line.getBounds());
        
        this.info = {
          distance: data.distance,
          computationTime: data.computationTime,
          pathCount: data.path.length
        };
      },
      error: (err) => console.error(err)
    });
  }

  
}