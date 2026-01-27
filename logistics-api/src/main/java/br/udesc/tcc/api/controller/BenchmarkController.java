package br.udesc.tcc.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.udesc.tcc.api.service.BenchmarkService;

@RestController
public class BenchmarkController {

    @Autowired
    private BenchmarkService benchmarkService;

    @GetMapping("/api/benchmark")
    public ResponseEntity<String> runBenchmark(@RequestParam(value = "iterations", defaultValue = "50") int iterations) {
        String csvContent = benchmarkService.runBenchmark(iterations);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"benchmark_results.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csvContent);
    }
}