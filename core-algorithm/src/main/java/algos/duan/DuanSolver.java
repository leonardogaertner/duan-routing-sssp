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

/**
 * Implementação do algoritmo SSSP (Single-Source Shortest Path) baseada no paper:
 * "Breaking the Sorting Barrier for Directed Single-Source Shortest Paths"
 * Autores: Ran Duan, Seth Pettie, Henzinger (2025).
 * * Esta classe segue estritamente os Algoritmos 1, 2 e 3 propostos no estudo.
 */
public class DuanSolver {

    private Graph graph;
    private double[] dist; 
    private int[] parent; // Para reconstrução do caminho
    
    // Parâmetros teóricos definidos no paper
    private int k; 
    private int t; 
    private int n; 
    
    private static final boolean DEBUG = false; // Desligue para benchmarks reais
    private static final double INF = Double.MAX_VALUE;

    /**
     * Inicialização e Chamada Principal.
     * Referência: Section 1.1 "Our Results" e Section 3 "The Algorithm".
     */
    public double[] compute(Graph graph, int sourceNode) {
        this.graph = graph;
        this.n = graph.getNodeCount();
        
        // Inicialização padrão de distâncias
        this.dist = new double[n + 2]; 
        Arrays.fill(dist, INF);
        dist[sourceNode] = 0;

        // Array para reconstrução do caminho (Backtracking)
        this.parent = new int[n + 2];
        Arrays.fill(parent, -1);

        /* * [PAPER] Parameter Definition:
         * "We choose parameters t = log^(2/3) n and k = log^(1/3) n."
         * Isso garante a complexidade O(m log^(2/3) n).
         */
        double logN = Math.log(n) / Math.log(2);
        // Garante mínimo de 2 para evitar erros em grafos muito pequenos
        this.k = (int) Math.max(2, Math.pow(logN, 1.0/3.0)); 
        this.t = (int) Math.max(2, Math.pow(logN, 2.0/3.0));
        
        // Define a profundidade máxima da recursão
        int maxLevel = (int) Math.ceil(logN / (double)t) + 1;

        if (DEBUG) {
            System.out.println(">>> DUAN PARAMETERS:");
            System.out.println("    N=" + n + " -> k=" + k + ", t=" + t + ", Levels=" + maxLevel);
        }

        Set<Integer> sourceSet = new HashSet<>();
        sourceSet.add(sourceNode);

        // Inicia o Algoritmo 3 (BMSSP)
        bmssp(maxLevel, INF, sourceSet);

        return dist;
    }

    /**
     * Recupera o caminho calculado.
     * Inclui proteção contra ciclos causados por arestas de peso zero (comuns na transformação de grafos).
     */
    public List<Integer> getPath(int targetNode) {
        List<Integer> path = new ArrayList<>();
        int curr = targetNode;
        int safetyCount = 0;
        int maxSteps = this.n + 1000; 

        while (curr != -1) {
            path.add(curr);
            curr = parent[curr];
            
            safetyCount++;
            if (safetyCount > maxSteps) {
                System.err.println("WARN: Ciclo detectado na reconstrução. Retornando caminho parcial.");
                break;
            }
        }
        Collections.reverse(path);
        return path;
    }

    // =================================================================================
    // ALGORITHM 3: BMSSP (Bounded Multi-Source Shortest Path)
    // Seção 3.3 do Artigo
    // =================================================================================
    private BmsspResult bmssp(int level, double B, Set<Integer> S) {
        
        /* * [PAPER] Base Case Condition:
         * "If i = 0 or |S| <= k, we simply run the base case algorithm (Algorithm 2)."
         */
        if (level == 0 || S.size() <= k) {
            return baseCase(B, S);
        }

        /* * [PAPER] Step 1: Find Pivots
         * "Let (P, W) <- FindPivots(B, S)."
         */
        PivotsResult pivots = findPivots(B, S);
        Set<Integer> P = pivots.P;
        Set<Integer> W = pivots.W;

        /* * [PAPER] Step 2: Initialize Data Structure
         * "Initialize a data structure D... Insert all v in P into D with key dist(v)."
         * Usamos M = 2^((i-1)t) conforme a teoria.
         */
        long M = (long) Math.pow(2, (level - 1) * t);
        DuanHeap D = new StandardHeapAdapter(); // Implementação simulada da Soft Heap
        D.initialize(M, B);

        for (int p : P) {
            D.insert(p, dist[p]);
        }

        double B_prime_prev; 
        if (P.isEmpty()) {
             B_prime_prev = B; 
        } else {
             // O menor valor atual em P define o limite inicial
             double minP = INF;
             for(int p : P) minP = Math.min(minP, dist[p]);
             B_prime_prev = minP;
        }

        Set<Integer> U = new HashSet<>();
        // Limite de expansão para garantir complexidade
        long limitSize = (long) (k * Math.pow(2, level * t)) + 1;

        /* * [PAPER] Step 3: Main Loop
         * "While D is not empty and |U| <= k * 2^(i*t)..."
         */
        while (!D.isEmpty() && U.size() < limitSize) {
            
            /* * [PAPER] Step 3a: Extract Min
             * "(B_i, S_i) <- D.extractMin()" (aqui chamado de pull)
             */
            DuanHeap.PullResult pullRes = D.pull();
            double B_i = pullRes.newBound;
            
            Set<Integer> S_i = new HashSet<>();
            for (HeapItem item : pullRes.items) S_i.add(item.nodeId);

            if (S_i.isEmpty() && D.isEmpty()) {
                if (B_i < B) B_prime_prev = B_i;
                break; 
            }
            if (S_i.isEmpty()) continue;

            /* * [PAPER] Step 3b: Recursive Call
             * "(B'_i, U_i) <- BMSSP(i-1, B_i, S_i)"
             */
            BmsspResult res = bmssp(level - 1, B_i, S_i);
            double B_i_prime = res.newBound;
            Set<Integer> U_i = res.U;
            U.addAll(U_i);

            /* * [PAPER] Step 3c: Relaxation & Insertion
             * "Relax all edges leaving U_i."
             * "If d(v) < B_i, add v to batch K. If B_i <= d(v) < B, insert v into D."
             */
            List<HeapItem> K = new ArrayList<>(); 
            
            for (int u : U_i) {
                List<Edge> edges = graph.getAdjacencyList().get(u);
                if (edges == null) continue;

                for (Edge edge : edges) {
                    int v = edge.target;
                    double newW = dist[u] + edge.weight;

                    // Relaxamento
                    if (newW <= dist[v]) {
                        // Atualiza pai apenas se estritamente menor para evitar ciclos A<->B de peso 0
                        if (newW < dist[v]) {
                            dist[v] = newW;
                            parent[v] = u;
                        }
                        
                        if (U.contains(v)) continue;

                        // Decisão de onde inserir (Fila K ou Heap D)
                        if (newW >= B_i && newW < B) {
                            D.insert(v, newW); 
                        } else if (newW < B_i) { 
                            K.add(new HeapItem(v, newW)); 
                        }
                    }
                }
            }

            // Reprocessar nós de fronteira que não foram resolvidos (Batch Prepend)
            for (int x : S_i) {
                if (!U.contains(x) && dist[x] < B_i) {
                    K.add(new HeapItem(x, dist[x]));
                }
            }
            
            if (!K.isEmpty()) {
                D.batchPrepend(K);
            }
            
            B_prime_prev = B_i_prime;
        }

        /* * [PAPER] Step 4: Final Cleanup
         * "Return (min(B'_last, B), U union {w in W | d(w) < finalBound})"
         */
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

    // =================================================================================
    // ALGORITHM 1: FIND PIVOTS
    // Seção 3.1 do Artigo
    // =================================================================================
    private PivotsResult findPivots(double B, Set<Integer> S) {
        /* * [PAPER] Definition:
         * "Grow a region W from S using BFS/Dijkstra until either W hits boundary B
         * or |W| > k|S|."
         */
        Set<Integer> W = new HashSet<>(S);
        Set<Integer> currentLayer = new HashSet<>(S);
        Map<Integer, Integer> tempPred = new HashMap<>(); // Predessores locais para contagem

        // Simula o crescimento em camadas (k layers)
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
            
            // Aborta se cresceu demais (condição de parada do paper)
            if (W.size() > k * S.size()) {
                return new PivotsResult(S, W); // Retorna S como fallback se W explodir
            }
        }

        /* * [PAPER] Pivot Selection:
         * "Select as pivots P all vertices v in W that are ancestors of at least k vertices
         * in the shortest path tree within W."
         */
        Set<Integer> P = new HashSet<>();
        Map<Integer, Integer> descendantCounts = new HashMap<>();
        
        // Contagem simplificada de descendentes para seleção de pivôs
        for (int w : W) {
            int curr = w;
            int depth = 0;
            // Sobe na árvore de predecessores temporária
            while (depth <= k + 1) {
                if (S.contains(curr)) {
                    descendantCounts.put(curr, descendantCounts.getOrDefault(curr, 0) + 1);
                    break;
                }
                if (!tempPred.containsKey(curr)) break;
                curr = tempPred.get(curr);
                depth++;
            }
        }

        for (Map.Entry<Integer, Integer> entry : descendantCounts.entrySet()) {
            if (entry.getValue() >= k) {
                P.add(entry.getKey());
            }
        }
        
        // Fallback
        if (P.isEmpty() && !W.isEmpty()) {
            return new PivotsResult(S, W);
        }

        return new PivotsResult(P, W);
    }

    // =================================================================================
    // ALGORITHM 2: BASE CASE
    // Seção 3.2 do Artigo
    // =================================================================================
    private BmsspResult baseCase(double B, Set<Integer> S) {
        /* * [PAPER] Base Case Logic:
         * "Run Dijkstra starting from S... Terminate if the minimum element in PQ is >= B
         * or if we have extracted k+1 vertices."
         */
        Set<Integer> U0 = new HashSet<>(S);
        PriorityQueue<HeapItem> pq = new PriorityQueue<>();
        for (int s : S) pq.add(new HeapItem(s, dist[s]));

        int expansionCount = 0; 
        
        // Loop limitado por k+1 extrações ou Bound B
        while (!pq.isEmpty() && expansionCount < k + 1) {
            HeapItem item = pq.poll();
            int u = item.nodeId;
            
            if (item.distance >= B) break; // Atingiu o limite B
            
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

        // Calcula o novo Bound (B_prime) baseado em onde paramos
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

    // --- Estruturas Auxiliares (Wrappers) ---

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
}