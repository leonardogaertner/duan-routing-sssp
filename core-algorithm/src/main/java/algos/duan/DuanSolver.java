package algos.duan;

import java.util.*;
import graph.Graph;
import graph.Graph.Edge;
import structures.*;
import algos.duan.DuanResults.*;
import it.unimi.dsi.fastutil.ints.*;

public class DuanSolver {

    private DuanContext ctx;
    private FindPivotsAlgorithm algo1;
    private BaseCaseAlgorithm algo2;
    
    // DEBUG STATS
    public static long timePivots = 0;
    public static long timePull = 0;
    public static long timeRelax = 0;
    public static long timeBatch = 0;
    public static int calls = 0;

    public double[] compute(Graph graph, int sourceNode) {
        this.ctx = new DuanContext(graph, sourceNode);
        this.algo1 = new FindPivotsAlgorithm(ctx);
        this.algo2 = new BaseCaseAlgorithm(ctx);
        
        timePivots = 0; timePull = 0; timeRelax = 0; timeBatch = 0; calls = 0;
        
        IntSet sourceSet = new IntOpenHashSet();
        sourceSet.add(sourceNode);

        long start = System.nanoTime();
        bmssp(ctx.getMaxLevel(), DuanContext.INF, sourceSet);
        long end = System.nanoTime();
        
        System.out.println("--- DUAN PROFILING ---");
        System.out.println("Total Time: " + (end - start)/1e6 + " ms");
        System.out.println("Pivots (Algo1): " + timePivots/1e6 + " ms");
        System.out.println("Pull (Heap): " + timePull/1e6 + " ms");
        System.out.println("Relaxation: " + timeRelax/1e6 + " ms");
        System.out.println("Batch Prepend: " + timeBatch/1e6 + " ms");
        System.out.println("Recursion Calls: " + calls);
        System.out.println("----------------------");

        return ctx.dist;
    }
    
    public List<Integer> getPath(int targetNode) {
        // ... (Mesma implementação anterior)
        List<Integer> path = new ArrayList<>();
        int curr = targetNode;
        int safetyCount = 0;
        int maxSteps = ctx.n + 1000; 
        while (curr != -1) {
            path.add(curr);
            curr = ctx.parent[curr];
            safetyCount++;
            if (safetyCount > maxSteps) break;
        }
        Collections.reverse(path);
        return path;
    }

    private BmsspResult bmssp(int level, double B, IntSet S) {
        calls++;
        if (level == 0) return algo2.execute(B, S);

        long t0 = System.nanoTime();
        PivotsResult pivots = algo1.execute(B, S);
        timePivots += (System.nanoTime() - t0);
        
        IntSet P = pivots.P;
        IntSet W = pivots.W;

        long M = (long) Math.pow(2, (level - 1) * ctx.t);
        DuanHeap D = new BlockPriorityQueue(); 
        D.initialize(M, B);

        IntIterator pIterator = P.iterator();
        while(pIterator.hasNext()) {
            int p = pIterator.nextInt();
            D.insert(p, ctx.dist[p]);
        }

        double B_prime_prev; 
        if (P.isEmpty()) {
             B_prime_prev = B; 
             // [FIX] Se P é vazio, S precisa ser processado de alguma forma.
             // O BatchPrepend no loop cuidará de S, mas precisamos entrar no loop.
             // Se S for pequeno e D vazio, o loop pula. 
             // Adicionamos S ao Heap se P falhar? Não, S vai para K via S_i.
             // Mas se D vazio, S_i nunca é gerado.
             // Fallback: Se P vazio, inserimos S diretamente em D para iniciar.
             IntIterator sIt = S.iterator();
             while(sIt.hasNext()) {
                 int s = sIt.nextInt();
                 if (!P.contains(s)) D.insert(s, ctx.dist[s]);
             }
        } else {
             double minP = DuanContext.INF;
             pIterator = P.iterator();
             while(pIterator.hasNext()) minP = Math.min(minP, ctx.dist[pIterator.nextInt()]);
             B_prime_prev = minP;
        }

        IntSet U = new IntOpenHashSet();
        long limitSize = (long) (ctx.k * Math.pow(2, level * ctx.t)) + 1;

        while (!D.isEmpty() && U.size() < limitSize) {
            
            t0 = System.nanoTime();
            DuanHeap.PullResult pullRes = D.pull();
            timePull += (System.nanoTime() - t0);
            
            double B_i = pullRes.newBound;
            
            IntSet S_i = new IntOpenHashSet();
            for (HeapItem item : pullRes.items) S_i.add(item.nodeId);

            if (S_i.isEmpty() && D.isEmpty()) {
                if (B_i < B) B_prime_prev = B_i;
                break; 
            }
            if (S_i.isEmpty()) continue;

            // RECURSÃO
            BmsspResult res = bmssp(level - 1, B_i, S_i);
            
            double B_i_prime = res.newBound;
            IntSet U_i = res.U;
            U.addAll(U_i);

            // [CRÍTICO] Reintegra a Fronteira Ativa (Leftovers) dos filhos
            if (res.activeFrontier != null) {
                IntIterator frontIt = res.activeFrontier.iterator();
                while(frontIt.hasNext()) {
                    int node = frontIt.nextInt();
                    // Se não foi finalizado e é promissor, volta pro Heap
                    if (!U.contains(node) && ctx.dist[node] < B) {
                        D.insert(node, ctx.dist[node]);
                    }
                }
            }

            t0 = System.nanoTime();
            List<HeapItem> K = new ArrayList<>(); 
            IntIterator uIterator = U_i.iterator();
            while(uIterator.hasNext()) {
                int u = uIterator.nextInt();
                List<Edge> edges = ctx.graph.getAdjacencyList().get(u);
                if (edges == null) continue;

                for (Edge edge : edges) {
                    int v = edge.target;
                    double newW = ctx.dist[u] + edge.weight;

                    if (newW <= ctx.dist[v]) {
                        ctx.dist[v] = newW;
                        ctx.parent[v] = u;
                        
                        if (U.contains(v)) continue;

                        if (newW >= B_i && newW < B) {
                            D.insert(v, newW); 
                        } else if (newW < B_i) { 
                            K.add(new HeapItem(v, newW)); 
                        }
                    }
                }
            }
            timeRelax += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            IntIterator sIterator = S_i.iterator();
            while(sIterator.hasNext()) {
                int x = sIterator.nextInt();
                if (!U.contains(x) && ctx.dist[x] < B_i) {
                    K.add(new HeapItem(x, ctx.dist[x]));
                }
            }
            if (!K.isEmpty()) D.batchPrepend(K);
            timeBatch += (System.nanoTime() - t0);
            
            B_prime_prev = B_i_prime;
        }

        double finalBound;
        if (D.isEmpty() && U.size() < limitSize) finalBound = B; 
        else finalBound = Math.min(B_prime_prev, B);

        IntIterator wIterator = W.iterator();
        while(wIterator.hasNext()) {
            int w = wIterator.nextInt();
            if (ctx.dist[w] < finalBound) U.add(w);
        }
        
        // Coleta o que sobrou neste nível para retornar ao pai
        IntSet myFrontier = D.drain();
        // Adiciona W \ U (nós alcançados na fase de Pivots mas não finalizados)
        wIterator = W.iterator();
        while(wIterator.hasNext()) {
            int w = wIterator.nextInt();
            if (!U.contains(w)) myFrontier.add(w);
        }

        return new BmsspResult(finalBound, U, myFrontier);
    }
}