package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    // Mapa para guardar os nós (ID -> Objeto Node)
    private final Map<Integer, Node> nodes = new HashMap<>();
    
    // Lista de Adjacência: ID do nó -> Lista de arestas saindo dele
    // Usamos Map aqui para facilitar, mas no futuro array é mais rápido
    private final Map<Integer, List<Edge>> adjacencyList = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.id, node);
        adjacencyList.putIfAbsent(node.id, new ArrayList<>());
    }

    public void addEdge(int from, int to, int weight) {
        // Verifica se os nós existem antes de criar a aresta
        if (nodes.containsKey(from) && nodes.containsKey(to)) {
            adjacencyList.get(from).add(new Edge(to, weight));
        }
    }

    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }
    
    // Classe interna para representar a aresta
    public static class Edge {
        public final int target;
        public final int weight; // Tempo de viagem

        public Edge(int target, int weight) {
            this.target = target;
            this.weight = weight;
        }

		public int getTarget() {
			return target;
		}

		public int getWeight() {
			return weight;
		}
    }

	public Map<Integer, Node> getNodes() {
		return nodes;
	}

	public Map<Integer, List<Edge>> getAdjacencyList() {
		return adjacencyList;
	}
    
     
}