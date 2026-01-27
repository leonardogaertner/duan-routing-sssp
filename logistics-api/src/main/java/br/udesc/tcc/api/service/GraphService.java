package br.udesc.tcc.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import algos.DuanSolver;
import br.udesc.tcc.api.dto.RouteResponse;
import graph.Graph;
import graph.GraphTransformer;
import graph.Node;
import io.DimacsLoader;
import jakarta.annotation.PostConstruct; // Se usar Java 17+, senão javax.annotation

@Service
public class GraphService {

	private Graph graph; // O grafo carregado na RAM

	// Caminhos fixos para o TCC (pode mover para application.properties depois)
	private static final String PATH_GR = "C:/dados/USA-road-t.NY.gr";
	private static final String PATH_CO = "C:/dados/USA-road-d.NY.co";

	@PostConstruct
	public void init() {
		try {
			System.out.println("--- [GraphService] Iniciando carga de dados ---");
			long start = System.currentTimeMillis();

			// 1. Carrega do disco
			DimacsLoader loader = new DimacsLoader();
			Graph rawGraph = loader.load(PATH_GR, PATH_CO);

			// 2. Transforma (Grau Constante)
			// Isso é CRUCIAL: O DuanSolver só funciona no grafo transformado
			GraphTransformer transformer = new GraphTransformer();
			this.graph = transformer.toConstantDegree(rawGraph);

			long end = System.currentTimeMillis();
			System.out.println("--- [GraphService] Grafo carregado em " + (end - start) + "ms. Pronto para rotas! ---");

		} catch (IOException e) {
			throw new RuntimeException("Falha fatal ao carregar grafo do TCC", e);
		}
	}

	public RouteResponse calculateRoute(int startNodeId, int endNodeId) {
		// Validação básica
		if (graph == null)
			throw new IllegalStateException("Grafo ainda não carregou!");

		// 1. Instancia o Solver
		DuanSolver solver = new DuanSolver();

		// 2. Roda o Algoritmo (Benchmark)
		long startTime = System.nanoTime();
		double[] distances = solver.compute(graph, startNodeId);
		long endTime = System.nanoTime();

		// 3. Recupera o Caminho (Path Reconstruction)
		List<Integer> nodeIds = solver.getPath(endNodeId);

		// 4. Converte IDs para Lat/Long (para o Frontend)
		List<RouteResponse.NodeDto> pathCoords = new ArrayList<>();
		for (int id : nodeIds) {
			// O grafo transformado preservou os objetos Node com lat/lon corretas
			Node node = graph.getNodes().get(id);
			if (node != null) {
				pathCoords.add(new RouteResponse.NodeDto(node.latitude, node.longitude));
			}
		}

		// 5. Monta a Resposta
		RouteResponse response = new RouteResponse();
		response.distance = distances[endNodeId];
		response.computationTime = (endTime - startTime); // Nanosegundos
		response.path = pathCoords;

		return response;
	}

	public Graph getGraph() {
		return graph;
	}

}