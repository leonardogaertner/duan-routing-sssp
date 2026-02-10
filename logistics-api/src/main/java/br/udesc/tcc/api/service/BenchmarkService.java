package br.udesc.tcc.api.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import algos.DijkstraSolver;
import algos.duan.DuanSolver;
import graph.Graph;

@Service
public class BenchmarkService {

    @Autowired
    private GraphService graphService; // Para pegar o grafo já carregado

    public String runBenchmark(int iterations) {
        Graph graph = graphService.getGraph(); 
        if (graph == null) return "Erro: Grafo não carregado.";

        StringBuilder csv = new StringBuilder();
        csv.append("Run ID;Source;Target;Distance;Dijkstra Time (ms);Duan Time (ms);Speedup (x)\n");

        Random rand = new Random();
        int maxNode = graph.getNodeCount();
        
        // Warmup (Aquecimento da JVM) - Importante para benchmarks Java
        System.out.println("Aquecendo JVM...");
        runSingleComparison(graph, 1, 500); 
        runSingleComparison(graph, 100, 2000);

        System.out.println("Iniciando Benchmark de " + iterations + " rodadas...");
        
        for (int i = 1; i <= iterations; i++) {
            // Sorteia nós válidos
            int source = rand.nextInt(maxNode);
            int target = rand.nextInt(maxNode);

            // Evita nós isolados ou inválidos (loop simples)
            while (graph.getAdjacencyList().get(source) == null || graph.getAdjacencyList().get(source).isEmpty()) {
                source = rand.nextInt(maxNode);
            }

            BenchmarkResult res = runSingleComparison(graph, source, target);
            
            // Só registra se encontrou caminho (para não poluir com INFINITY)
            if (res.distance < Double.MAX_VALUE) {
                double speedup = res.dijkstraTime / res.duanTime;
                
                String line = String.format("%d;%d;%d;%.2f;%.4f;%.4f;%.2f", 
                        i, source, target, res.distance, res.dijkstraTime, res.duanTime, speedup);
                
                csv.append(line).append("\n");
                System.out.println("Run " + i + ": Speedup " + String.format("%.2fx", speedup));
            } else {
                i--; // Tenta de novo se caiu em caminho impossível
            }
        }
        
        return csv.toString();
    }

    private BenchmarkResult runSingleComparison(Graph graph, int s, int t) {
        // 1. Dijkstra
        DijkstraSolver dijkstra = new DijkstraSolver();
        long startD = System.nanoTime();
        double distD = dijkstra.compute(graph, s, t);
        long endD = System.nanoTime();

        // 2. Duan
        DuanSolver duan = new DuanSolver();
        long startDu = System.nanoTime();
        // Duan calcula 1-para-todos, pegamos o destino específico
        double[] distsDu = duan.compute(graph, s); 
        long endDu = System.nanoTime();

        double timeD = (endD - startD) / 1_000_000.0; // ms
        double timeDu = (endDu - startDu) / 1_000_000.0; // ms
        
        return new BenchmarkResult(distD, timeD, timeDu);
    }

    private static class BenchmarkResult {
        double distance;
        double dijkstraTime;
        double duanTime;
        public BenchmarkResult(double d, double dt, double dut) {
            this.distance = d; this.dijkstraTime = dt; this.duanTime = dut;
        }
    }
}