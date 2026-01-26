package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graph.Graph.Edge;

public class GraphTransformer {

    // Mapeia: ID Original -> Lista de IDs Virtuais que compõem o ciclo
    // Isso será vital para depois "traduzir" a rota do Duan de volta para o mapa real.
    private final Map<Integer, List<Integer>> virtualMapping = new HashMap<>();

    /**
     * Transforma um grafo arbitrário em um grafo de grau constante (máx 2 in/out),
     * conforme descrito na seção 2 (Preliminaries) do paper.
     */
    public Graph toConstantDegree(Graph originalGraph) {
        Graph newGraph = new Graph();
        int nextVirtualId = 1; // IDs virtuais começam do 1 incrementalmente

        // Passo 1: Criar os Ciclos de Nós Virtuais
        // Para cada nó do grafo original...
        for (int originalId = 1; originalId <= originalGraph.getNodeCount(); originalId++) {
            List<Edge> outgoing = originalGraph.getAdjacencyList().getOrDefault(originalId, new ArrayList<>());
            List<Integer> cycleIds = new ArrayList<>();

            // Se o nó original não tem saídas, criamos 1 nó virtual apenas para existir
            if (outgoing.isEmpty()) {
                // Mantemos a coordenada original para fins de debug (opcional)
                Node originalNode = originalGraph.getNodes().get(originalId);
                newGraph.addNode(new Node(nextVirtualId, (long)(originalNode.latitude*1000000), (long)(originalNode.longitude*1000000)));
                cycleIds.add(nextVirtualId++);
            } else {
                // Se tem saídas, cria um nó virtual para cada aresta de saída
                for (int i = 0; i < outgoing.size(); i++) {
                    Node originalNode = originalGraph.getNodes().get(originalId);
                    // Cria nó virtual copiando a lat/long do original (todos no mesmo lugar geográfico)
                    newGraph.addNode(new Node(nextVirtualId, (long)(originalNode.latitude*1000000), (long)(originalNode.longitude*1000000)));
                    cycleIds.add(nextVirtualId++);
                }

                // Conectar o ciclo interno com peso 0 
                for (int i = 0; i < cycleIds.size(); i++) {
                    int currentVirtual = cycleIds.get(i);
                    int nextVirtual = cycleIds.get((i + 1) % cycleIds.size()); // Fecha o ciclo
                    newGraph.addEdge(currentVirtual, nextVirtual, 0);
                }
            }
            
            // Guardamos quais nós virtuais representam o nó original
            virtualMapping.put(originalId, cycleIds);
        }

        // Passo 2: Reconectar as Arestas Originais (Agora entre ciclos) [cite: 67]
        for (int originalU = 1; originalU <= originalGraph.getNodeCount(); originalU++) {
            List<Edge> edges = originalGraph.getAdjacencyList().get(originalU);
            if (edges == null) continue;

            List<Integer> uCycle = virtualMapping.get(originalU);

            // Para cada aresta original (u -> v) com peso w
            for (int i = 0; i < edges.size(); i++) {
                Edge edge = edges.get(i);
                int originalV = edge.target;
                int weight = edge.weight;

                // Pegamos o nó virtual específico do ciclo de U responsável por essa saída
                int uVirtual = uCycle.get(i);

                // Pegamos QUALQUER nó virtual do ciclo de V para ser a entrada
                // (Por simplicidade, pegamos o primeiro, pois o ciclo interno distribui com peso 0)
                List<Integer> vCycle = virtualMapping.get(originalV);
                if (vCycle != null && !vCycle.isEmpty()) {
                    int vVirtual = vCycle.get(0);
                    
                    // Adiciona a aresta com o peso original [cite: 67]
                    newGraph.addEdge(uVirtual, vVirtual, weight);
                }
            }
        }

        return newGraph;
    }
    
    public Map<Integer, List<Integer>> getVirtualMapping() {
        return virtualMapping;
    }
}