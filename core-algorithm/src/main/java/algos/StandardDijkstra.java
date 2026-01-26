package algos;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import graph.Graph;
import graph.Graph.Edge;
import structures.HeapItem;

public class StandardDijkstra {
    
    public double[] compute(Graph graph, int sourceNode) {
        int n = graph.getNodeCount();
        double[] dist = new double[n + 2];
        Arrays.fill(dist, Double.MAX_VALUE);
        dist[sourceNode] = 0;
        
        PriorityQueue<HeapItem> pq = new PriorityQueue<>();
        pq.add(new HeapItem(sourceNode, 0));
        
        while(!pq.isEmpty()) {
            HeapItem item = pq.poll();
            int u = item.nodeId;
            
            if (item.distance > dist[u]) continue;
            
            List<Edge> edges = graph.getAdjacencyList().get(u);
            if (edges != null) {
                for (Edge e : edges) {
                    if (dist[u] + e.weight < dist[e.target]) {
                        dist[e.target] = dist[u] + e.weight;
                        pq.add(new HeapItem(e.target, dist[e.target]));
                    }
                }
            }
        }
        return dist;
    }
}