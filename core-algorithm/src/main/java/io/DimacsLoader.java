package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import graph.Graph;
import graph.Node;

public class DimacsLoader {

    public Graph load(String pathGr, String pathCo) throws IOException {
        Graph graph = new Graph();
        
        System.out.println("Carregando coordenadas de: " + pathCo);
        // 1. Ler Coordenadas (.co)
        try (BufferedReader br = new BufferedReader(new FileReader(pathCo))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) { 
                    // Formato: v ID Longitude Latitude
                    String[] parts = line.split("\\s+"); // divide por espaços
                    int id = Integer.parseInt(parts[1]);
                    long lon = Long.parseLong(parts[2]); // DIMACS põe longitude antes
                    long lat = Long.parseLong(parts[3]);
                    
                    graph.addNode(new Node(id, lat, lon));
                }
            }
        }

        System.out.println("Carregando arestas de: " + pathGr);
        // 2. Ler Arestas/Pesos (.gr)
        try (BufferedReader br = new BufferedReader(new FileReader(pathGr))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("a ")) {
                    // Formato: a Origem Destino Peso
                    String[] parts = line.split("\\s+");
                    int u = Integer.parseInt(parts[1]);
                    int v = Integer.parseInt(parts[2]);
                    int w = Integer.parseInt(parts[3]);
                    
                    graph.addEdge(u, v, w);
                }
            }
        }
        
        return graph;
    }
}