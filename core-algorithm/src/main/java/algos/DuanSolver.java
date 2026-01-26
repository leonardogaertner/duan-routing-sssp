package algos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import graph.Graph;
import graph.Graph.Edge;
import structures.DuanHeap;
import structures.HeapItem;
import structures.StandardHeapAdapter;

public class DuanSolver {

    private Graph graph;
    private double[] dist; 
    private int k; 
    private int t; 
    private int n; 
    private int[] parent;
    
    // FLAG DE DEBUG (Desligar depois)
    private static final boolean DEBUG = true;

    private static final double INF = Double.MAX_VALUE;

    public double[] compute(Graph graph, int sourceNode) {
        this.graph = graph;
        this.n = graph.getNodeCount();
        this.dist = new double[n + 2]; 
        Arrays.fill(dist, INF);
        dist[sourceNode] = 0;
        this.parent = new int[n + 2]; // Inicializa
        Arrays.fill(parent, -1);      // -1 indica sem pai

        // Configuração Manual para Teste (Forçando valores mais altos)
        this.k = 20;  
        this.t = 10;
        double logN = Math.log(n) / Math.log(2); 
        int maxLevel = (int) Math.ceil(logN / (double)t) + 1;

        if (DEBUG) {
            System.out.println(">>> DEBUG START: DuanSolver");
            System.out.println(">>> Config: N=" + n + " k=" + k + " t=" + t + " MaxLevel=" + maxLevel);
            System.out.println(">>> Origem: " + sourceNode);
        }

        Set<Integer> sourceSet = new HashSet<>();
        sourceSet.add(sourceNode);

        bmssp(maxLevel, INF, sourceSet);

        return dist;
    }

    private BmsspResult bmssp(int level, double B, Set<Integer> S) {
        if (DEBUG && level >= 1) {
            System.out.println("  [BMSSP] Lvl=" + level + " |S|=" + S.size() + " Bound=" + (B == INF ? "INF" : B));
        }

        if (level == 0) {
            return baseCase(B, S);
        }

        // --- STEP 1: FIND PIVOTS ---
        PivotsResult pivots = findPivots(B, S);
        Set<Integer> P = pivots.P;
        Set<Integer> W = pivots.W;

        // --- STEP 2: SETUP HEAP ---
        long M = (long) Math.pow(2, (level - 1) * t);
        DuanHeap D = new StandardHeapAdapter(); 
        D.initialize(M, B);

        for (int p : P) {
            D.insert(p, dist[p]);
        }

        double B_prime_prev; 
        if (P.isEmpty()) {
             B_prime_prev = B; 
        } else {
             double minP = INF;
             for(int p : P) minP = Math.min(minP, dist[p]);
             B_prime_prev = minP;
        }

        Set<Integer> U = new HashSet<>();
        long limitSize = (long) (k * Math.pow(2, level * t)) + 1;

        // --- STEP 3: MAIN LOOP ---
        int loopCount = 0;
        while (!D.isEmpty() && U.size() < limitSize) {
            loopCount++;
            
            DuanHeap.PullResult pullRes = D.pull();
            double B_i = pullRes.newBound;
            
            Set<Integer> S_i = new HashSet<>();
            for (HeapItem item : pullRes.items) S_i.add(item.nodeId);

            if (S_i.isEmpty() && D.isEmpty()) {
                if (B_i < B) B_prime_prev = B_i;
                break; 
            }
            if (S_i.isEmpty()) continue;

            // Recurse
            BmsspResult res = bmssp(level - 1, B_i, S_i);
            double B_i_prime = res.newBound;
            Set<Integer> U_i = res.U;
            U.addAll(U_i);

            // Relaxamento
            List<HeapItem> K = new ArrayList<>(); 
            
            for (int u : U_i) {
                List<Edge> edges = graph.getAdjacencyList().get(u);
                if (edges == null) continue;

                for (Edge edge : edges) {
                    int v = edge.target;
                    double newW = dist[u] + edge.weight;

                    // CORREÇÃO FINAL: Usamos <= para pegar atualizações do filho
                    // MAS filtramos !U.contains(v) para evitar loop em nós já finalizados
                    if (newW <= dist[v]) {
                        dist[v] = newW; 
                        parent[v] = u;
                        
                        if (U.contains(v)) continue; // CRUCIAL: Impede loop infinito em nós resolvidos
                        
                        // Lógica de inserção
                        if (newW >= B_i && newW < B) {
                            D.insert(v, newW); 
                        } else if (newW < B_i) { 
                            K.add(new HeapItem(v, newW)); 
                        }
                    }
                }
            }

            // Batch Prepend (Reinsere nós da fronteira não processados)
            for (int x : S_i) {
                // Se o nó estava na lista S_i mas não foi resolvido pelo filho (não está em U),
                // ele deve voltar para o lote K se estiver dentro do range de interesse
                if (!U.contains(x) && dist[x] < B_i) { 
                    K.add(new HeapItem(x, dist[x]));
                }
            }
            
            if (!K.isEmpty()) {
                D.batchPrepend(K);
            }
            
            B_prime_prev = B_i_prime;
        }

        double finalBound;
        if (D.isEmpty() && U.size() < limitSize) {
            finalBound = B; 
        } else {
            finalBound = Math.min(B_prime_prev, B);
        }

        for (int w : W) {
            if (dist[w] < finalBound) {
                U.add(w);
            }
        }

        return new BmsspResult(finalBound, U);
    }

    private PivotsResult findPivots(double B, Set<Integer> S) {
        Set<Integer> W = new HashSet<>(S);
        Set<Integer> currentLayer = new HashSet<>(S);
        Map<Integer, Integer> tempPred = new HashMap<>();

        for (int i = 1; i <= k; i++) {
            Set<Integer> nextLayer = new HashSet<>();
            for (int u : currentLayer) {
                List<Edge> edges = graph.getAdjacencyList().get(u);
                if (edges == null) continue;

                for (Edge edge : edges) {
                    int v = edge.target;
                    
                    if (dist[u] + edge.weight < dist[v]) { 
                        dist[v] = dist[u] + edge.weight;
                        parent[v] = u;
                        tempPred.put(v, u); 
                        if (dist[v] < B) {
                            nextLayer.add(v);
                            W.add(v);
                        }
                    }
                }
            }
            currentLayer = nextLayer;
            if (W.size() > k * S.size()) {
                if (DEBUG) System.out.println("    [FindPivots] W cresceu demais (" + W.size() + "). Retornando S como pivô.");
                return new PivotsResult(S, W);
            }
        }

        Set<Integer> P = new HashSet<>();
        Map<Integer, Integer> rootCounts = new HashMap<>();
        
        for (int w : W) {
            int curr = w;
            int depth = 0;
            while (depth <= k + 1) {
                if (S.contains(curr)) {
                    rootCounts.put(curr, rootCounts.getOrDefault(curr, 0) + 1);
                    break;
                }
                if (!tempPred.containsKey(curr)) break;
                curr = tempPred.get(curr);
                depth++;
            }
        }

        for (Map.Entry<Integer, Integer> entry : rootCounts.entrySet()) {
            if (entry.getValue() >= k) {
                P.add(entry.getKey());
            }
        }
        
        // LOGICA DE FALLBACK CRÍTICA
        if (P.isEmpty() && !W.isEmpty()) {
            if (DEBUG) System.out.println("    [FindPivots] Nenhum pivô qualificado encontrado, mas W existe. Forçando S.");
            return new PivotsResult(S, W);
        }

        return new PivotsResult(P, W);
    }
    
    // ... baseCase e Classes internas mantidas iguais ...
    private BmsspResult baseCase(double B, Set<Integer> S) {
        Set<Integer> U0 = new HashSet<>(S);
        PriorityQueue<HeapItem> pq = new PriorityQueue<>();
        for (int s : S) pq.add(new HeapItem(s, dist[s]));

        int expansionCount = 0; 
        while (!pq.isEmpty() && expansionCount < k + 1) {
            HeapItem item = pq.poll();
            int u = item.nodeId;
            if (item.distance >= B) break;
            
            if (!U0.contains(u)) {
                U0.add(u);
                expansionCount++;
            }

            List<Edge> edges = graph.getAdjacencyList().get(u);
            if (edges != null) {
                for (Edge edge : edges) {
                    int v = edge.target;
                    double newDist = dist[u] + edge.weight;
                    if (newDist < dist[v] && newDist < B) {
                        dist[v] = newDist;
                        parent[v] = u;
                        pq.add(new HeapItem(v, newDist)); 
                    }
                }
            }
        }
        double B_prime;
        Set<Integer> U;
        if (expansionCount <= k) {
            B_prime = B;
            U = U0;
        } else {
            double maxDist = 0;
            for (int u : U0) if (dist[u] < B) maxDist = Math.max(maxDist, dist[u]);
            B_prime = maxDist;
            U = new HashSet<>();
            for (int u : U0) {
                if (dist[u] < B_prime) U.add(u);
            }
        }
        return new BmsspResult(B_prime, U);
    }

    public static class BmsspResult {
        public final double newBound;
        public final Set<Integer> U;
        public BmsspResult(double newBound, Set<Integer> U) {
            this.newBound = newBound;
            this.U = U;
        }
    }

    private static class PivotsResult {
        public final Set<Integer> P;
        public final Set<Integer> W;
        public PivotsResult(Set<Integer> P, Set<Integer> W) {
            this.P = P;
            this.W = W;
        }
    }
    /**
     * Reconstrói o caminho do startNode até o targetNode.
     */
    public List<Integer> getPath(int targetNode) {
        List<Integer> path = new ArrayList<>();
        int curr = targetNode;

        // Backtracking: Vai do destino voltando para a origem usando o array parent
        while (curr != -1) {
            path.add(curr);
            curr = parent[curr];
        }

        // O caminho foi montado de trás para frente, então invertemos
        Collections.reverse(path);
        return path;
    }
}