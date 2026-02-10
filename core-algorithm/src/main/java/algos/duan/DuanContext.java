package algos.duan;

import graph.Graph;
import java.util.Arrays;

/**
 * Contexto compartilhado para manter o estado do algoritmo de Duan.
 * Evita a passagem excessiva de parâmetros entre os sub-algoritmos.
 */
public class DuanContext {
    public final Graph graph;
    public final int n;
    public final int k;
    public final int t;
    
    public final double[] dist;
    public final int[] parent;
    
    public static final double INF = Double.MAX_VALUE;

    public DuanContext(Graph graph, int sourceNode) {
        this.graph = graph;
        this.n = graph.getNodeCount();
        
        // Inicialização de arrays
        this.dist = new double[n + 2]; 
        Arrays.fill(dist, INF);
        dist[sourceNode] = 0;

        this.parent = new int[n + 2];
        Arrays.fill(parent, -1);

        // Cálculo dos parâmetros teóricos (Section 1.1)
        double logN = Math.log(n) / Math.log(2);
        this.k = (int) Math.max(2, Math.pow(logN, 1.0/3.0)); 
        this.t = (int) Math.max(2, Math.pow(logN, 2.0/3.0));
    }
    
    public int getMaxLevel() {
         double logN = Math.log(n) / Math.log(2);
         return (int) Math.ceil(logN / (double)t) + 1;
    }
}