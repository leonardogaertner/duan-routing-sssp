package algos.duan;

import java.util.List;

import algos.duan.DuanResults.PivotsResult;
import graph.Graph.Edge;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class FindPivotsAlgorithm {

    private final DuanContext ctx;

    public FindPivotsAlgorithm(DuanContext ctx) {
        this.ctx = ctx;
    }

    public PivotsResult execute(double B, IntSet S) {
        // Usa IntOpenHashSet para evitar boxing de Integer
        IntSet W = new IntOpenHashSet(S);
        IntSet currentLayer = new IntOpenHashSet(S);
        
        // Int2IntMap economiza muita memória comparado a Map<Integer, Integer>
        Int2IntMap tempPred = new Int2IntOpenHashMap(); 

        for (int i = 1; i <= ctx.k; i++) {
            IntSet nextLayer = new IntOpenHashSet();
            
            // O iterator de fastutil evita criação de objetos, mas o foreach simples também funciona bem
            for (int u : currentLayer) {
                List<Edge> edges = ctx.graph.getAdjacencyList().get(u);
                if (edges == null) continue;

                for (Edge edge : edges) {
                    int v = edge.target;
                    
                    // [CORREÇÃO PAPER - Remark 3.4] (Mantida)
                    if (ctx.dist[u] + edge.weight <= ctx.dist[v]) { 
                        ctx.dist[v] = ctx.dist[u] + edge.weight;
                        ctx.parent[v] = u; 
                        tempPred.put(v, u); 
                        
                        if (ctx.dist[v] < B) {
                            nextLayer.add(v);
                            W.add(v);
                        }
                    }
                }
            }
            currentLayer = nextLayer;
            
            if (W.size() > ctx.k * S.size()) {
                return new PivotsResult(S, W); 
            }
        }

        IntSet P = new IntOpenHashSet();
        Int2IntMap descendantCounts = new Int2IntOpenHashMap();
        
        for (int w : W) {
            int curr = w;
            int depth = 0;
            while (depth <= ctx.k + 1) {
                if (S.contains(curr)) {
                    descendantCounts.put(curr, descendantCounts.getOrDefault(curr, 0) + 1);
                    break;
                }
                if (!tempPred.containsKey(curr)) break;
                curr = tempPred.get(curr);
                depth++;
            }
        }

        for (Int2IntMap.Entry entry : descendantCounts.int2IntEntrySet()) {
            if (entry.getIntValue() >= ctx.k) {
                P.add(entry.getIntKey());
            }
        }
        
        if (P.isEmpty() && !W.isEmpty()) {
            return new PivotsResult(S, W);
        }

        return new PivotsResult(P, W);
    }
}