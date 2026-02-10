package algos.duan;

import graph.Graph.Edge;
import algos.duan.DuanResults.BmsspResult;
import it.unimi.dsi.fastutil.ints.*;
import java.util.List;

public class BaseCaseAlgorithm {

    private final DuanContext ctx;

    public BaseCaseAlgorithm(DuanContext ctx) {
        this.ctx = ctx;
    }

    public BmsspResult execute(double B, IntSet S) {
        IntSet U0 = new IntOpenHashSet(S);
        
        IntHeapPriorityQueue pq = new IntHeapPriorityQueue(new IntComparator() {
            @Override
            public int compare(int k1, int k2) {
                return Double.compare(ctx.dist[k1], ctx.dist[k2]);
            }
        });

        IntIterator sIterator = S.iterator();
        while (sIterator.hasNext()) {
            pq.enqueue(sIterator.nextInt());
        }

        int expansionCount = 0; 
        
        while (!pq.isEmpty() && expansionCount < ctx.k + 1) {
            int u = pq.dequeueInt();
            
            if (ctx.dist[u] >= B) {
                pq.enqueue(u); // Devolve para capturar no final
                break;
            }
            
            if (!U0.contains(u)) {
                U0.add(u);
                expansionCount++;
            }

            List<Edge> edges = ctx.graph.getAdjacencyList().get(u);
            if (edges != null) {
                for (Edge edge : edges) {
                    int v = edge.target;
                    double newDist = ctx.dist[u] + edge.weight;
                    boolean strictlyImproved = newDist < ctx.dist[v];

                    if (newDist <= ctx.dist[v] && newDist < B) {
                        ctx.dist[v] = newDist;
                        ctx.parent[v] = u;
                        if (strictlyImproved) pq.enqueue(v); 
                    }
                }
            }
        }

        double B_prime;
        IntSet U;
        IntSet frontier = new IntOpenHashSet(); // Captura a fronteira perdida

        if (expansionCount <= ctx.k) {
            B_prime = B;
            U = U0;
        } else {
            double maxDist = 0;
            IntIterator iterator = U0.iterator();
            while(iterator.hasNext()) {
                int u = iterator.nextInt();
                if (ctx.dist[u] < B) {
                    if (ctx.dist[u] > maxDist) maxDist = ctx.dist[u];
                }
            }
            B_prime = maxDist;
            
            U = new IntOpenHashSet();
            iterator = U0.iterator();
            while(iterator.hasNext()) {
                int u = iterator.nextInt();
                if (ctx.dist[u] < B_prime) U.add(u);
                else frontier.add(u); // Cortado pelo B_prime -> Fronteira
            }
        }
        
        // Adiciona tudo que restou na Fila de Prioridade
        while (!pq.isEmpty()) {
            frontier.add(pq.dequeueInt());
        }
        frontier.removeAll(U);

        return new BmsspResult(B_prime, U, frontier);
    }
}