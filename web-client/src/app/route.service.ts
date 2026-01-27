import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface NodeDto { lat: number; lon: number; }
export interface RouteResponse {
  distance: number;
  computationTime: number;
  path: NodeDto[];
}

@Injectable({ providedIn: 'root' })
export class RouteService {
  private apiUrl = 'http://localhost:8080/api/route'; // Seu servidor Java

  constructor(private http: HttpClient) { }

  getRoute(from: number, to: number): Observable<RouteResponse> {
    return this.http.get<RouteResponse>(`${this.apiUrl}?from=${from}&to=${to}`);
  }
}   