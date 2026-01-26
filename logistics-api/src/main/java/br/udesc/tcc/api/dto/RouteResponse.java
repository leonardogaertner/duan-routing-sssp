package br.udesc.tcc.api.dto;

import java.util.List;

public class RouteResponse {
    public double distance;      // Tempo total ou distância
    public long computationTime; // Tempo de CPU (para o benchmark do TCC)
    public List<NodeDto> path;   // A linha para desenhar no mapa

    public static class NodeDto {
        public double lat;
        public double lon;
        
        public NodeDto(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}