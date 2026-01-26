package br.udesc.tcc.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import br.udesc.tcc.api.dto.RouteResponse;
import br.udesc.tcc.api.service.GraphService;

@RestController
@RequestMapping("/api/route")
@CrossOrigin(origins = "*") // Permite que o Angular acesse sem bloqueio
public class RouteController {

    @Autowired
    private GraphService graphService;

    @GetMapping
    public RouteResponse getRoute(@RequestParam int from, @RequestParam int to) {
        // Exemplo de chamada: GET /api/route?from=1&to=5000
        System.out.println("Calculando rota de " + from + " para " + to);
        return graphService.calculateRoute(from, to);
    }
}