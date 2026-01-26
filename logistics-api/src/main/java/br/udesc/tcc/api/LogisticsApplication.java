package br.udesc.tcc.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsApplication.class, args);
        System.out.println("--- SERVIDOR LOGÍSTICO RODANDO NA PORTA 8080 ---");
    }
}