package main;

import java.io.IOException;

import algos.StandardDijkstra;
import algos.duan.DuanSolver;
import graph.Graph;
import graph.GraphTransformer;
import io.DimacsLoader;

public class Main {
    
    // ATENÇÃO: Ajuste para os seus caminhos reais
    private static final String PATH_CO = "C:/dados/USA-road-d.NY.co";
    private static final String PATH_GR = "C:/dados/USA-road-t.NY.gr";

    public static void main(String[] args) {
        try {
            System.out.println("=== INICIANDO BENCHMARK TCC DUAN ET AL. (2025) ===");
            
            // 1. Carga de Dados
            DimacsLoader loader = new DimacsLoader();
            System.out.print("Carregando grafo DIMACS... ");
            long tLoad = System.currentTimeMillis();
            Graph originalGraph = loader.load(PATH_GR, PATH_CO);
            System.out.println("OK (" + (System.currentTimeMillis() - tLoad) + "ms)");
            System.out.println("   Original: " + originalGraph.getNodeCount() + " nós, " + originalGraph.getEdgeCount() + " arestas.");

            // 2. Transformação (Grau Constante)
            System.out.print("Aplicando Transformação de Grau Constante... ");
            long tTrans = System.currentTimeMillis();
            GraphTransformer transformer = new GraphTransformer();
            Graph graph = transformer.toConstantDegree(originalGraph);
            System.out.println("OK (" + (System.currentTimeMillis() - tTrans) + "ms)");
            System.out.println("   Processado: " + graph.getNodeCount() + " nós (Virtual Nodes criados).");

            // 3. Definição do Cenário de Teste
            // Escolhemos um nó de origem aleatório dentro do range original válido
            int sourceNode = 1; 
            // Nó destino aleatório para imprimir exemplo (ex: nó do meio)
            int targetProbe = graph.getNodeCount() / 2; 

            System.out.println("\n--- RODADA DE TESTES (Origem: " + sourceNode + ") ---");

            // 4. Executa Dijkstra Padrão (Baseline)
            System.out.print("Rodando Dijkstra Padrão... ");
            long tDijkstraStart = System.nanoTime();
            StandardDijkstra dijkstra = new StandardDijkstra();
            double[] distDijkstra = dijkstra.compute(graph, sourceNode);
            long tDijkstraEnd = System.nanoTime();
            double timeDijkstraMs = (tDijkstraEnd - tDijkstraStart) / 1_000_000.0;
            System.out.printf("Concluído em %.2f ms\n", timeDijkstraMs);

            // 5. Executa Duan Solver (Novo Algoritmo)
            System.out.print("Rodando Duan Solver (2025)... ");
            long tDuanStart = System.nanoTime();
            DuanSolver duan = new DuanSolver();
            double[] distDuan = duan.compute(graph, sourceNode);
            long tDuanEnd = System.nanoTime();
            double timeDuanMs = (tDuanEnd - tDuanStart) / 1_000_000.0;
            System.out.printf("Concluído em %.2f ms\n", timeDuanMs);

            // 6. Validação de Resultados (Corretude)
            System.out.println("\n--- VALIDAÇÃO DE CORRETUDE ---");
            boolean correta = true;
            int erros = 0;
            double maxDiff = 0;
            
            // Verifica uma amostra de nós (ou todos)
            for (int i = 1; i <= graph.getNodeCount(); i++) {
                double d1 = distDijkstra[i];
                double d2 = distDuan[i];
                
                // Ignora nós inalcançáveis (Infinito) em ambos
                if (d1 == Double.MAX_VALUE && d2 == Double.MAX_VALUE) continue;
                
                // Se diferem por mais de 0.001 (erro de ponto flutuante), falhou
                if (Math.abs(d1 - d2) > 0.001) {
                    correta = false;
                    erros++;
                    maxDiff = Math.max(maxDiff, Math.abs(d1 - d2));
                    if (erros < 5) { // Mostra só os 5 primeiros erros
                        System.err.println("ERRO Nó " + i + ": Dijkstra=" + d1 + " vs Duan=" + d2);
                    }
                }
            }

            if (correta) {
                System.out.println("SUCESSO: Todas as distâncias conferem!");
                System.out.println("Distância até nó de prova (" + targetProbe + "): " + distDijkstra[targetProbe]);
            } else {
                System.err.println("FALHA: Encontrados " + erros + " nós com distâncias divergentes.");
                System.err.println("Maior diferença encontrada: " + maxDiff);
            }
            
            // 7. Resumo Comparativo
            System.out.println("\n--- RESULTADO FINAL ---");
            System.out.printf("Dijkstra: %.2f ms\n", timeDijkstraMs);
            System.out.printf("Duan 2025: %.2f ms\n", timeDuanMs);
            
            if (timeDuanMs < timeDijkstraMs) {
                System.out.println("RESULTADO: Duan foi " + String.format("%.1f", timeDijkstraMs/timeDuanMs) + "x mais rápido!");
            } else {
                System.out.println("RESULTADO: Duan foi mais lento (Esperado nesta fase sem otimização da BlockLinkedList).");
                System.out.println("Nota: A vantagem teórica só aparece com a estrutura de dados otimizada implementada.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro fatal na execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}