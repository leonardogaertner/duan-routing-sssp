package algos;

import java.util.Arrays;
import java.util.PriorityQueue;

import graph.Graph;
import graph.Graph.Edge;

public class DijkstraSolver {

	public double compute(Graph graph, int sourceNode, int targetNode) {
		int n = graph.getNodeCount();
		double[] dist = new double[n + 1];
		Arrays.fill(dist, Double.MAX_VALUE);
		dist[sourceNode] = 0;

		PriorityQueue<NodeDist> pq = new PriorityQueue<>();
		pq.add(new NodeDist(sourceNode, 0));

		while (!pq.isEmpty()) {
			NodeDist current = pq.poll();
			int u = current.node;

			if (u == targetNode)
				return dist[u]; // Otimização: paramos ao achar o destino
			if (current.dist > dist[u])
				continue;

			if (graph.getAdjacencyList().get(u) != null) {
				for (Edge e : graph.getAdjacencyList().get(u)) {
					int v = e.target;
					double newDist = dist[u] + e.weight;
					if (newDist < dist[v]) {
						dist[v] = newDist;
						pq.add(new NodeDist(v, newDist));
					}
				}
			}
		}
		return dist[targetNode];
	}

	private static class NodeDist implements Comparable<NodeDist> {
		int node;
		double dist;

		public NodeDist(int node, double dist) {
			this.node = node;
			this.dist = dist;
		}

		public int compareTo(NodeDist o) {
			return Double.compare(this.dist, o.dist);
		}
	}
}