# duan-routing-sssp

Implementação de alta performance do algoritmo SSSP de Duan et al. (2025) aplicada a roteamento logístico. Este projeto foi desenvolvido como Trabalho de Conclusão de Curso (TCC) em Engenharia de Software na UDESC.

## Sobre o Projeto

O objetivo deste sistema é validar a eficiência teórica do algoritmo de quebra de barreira de ordenação (Sorting Barrier) em grafos de grande escala transformados para grau constante. O sistema é capaz de calcular rotas em grafos DIMACS reais (ex: USA-road-d.NY).

## Estrutura dos Módulos

* **core-algorithm**: Biblioteca Java puro contendo a implementação matemática do Duan Solver, estruturas de dados customizadas (Soft Heaps simuladas) e transformadores de grafo.
* **logistics-api**: API REST desenvolvida com Spring Boot 3 que expõe o algoritmo para consumo externo e gerencia o carregamento dos dados em memória.

## Pré-requisitos

* Java 17 ou superior
* Maven 3.6 ou superior
* Arquivos de grafo DIMACS (.gr e .co) configurados localmente

## Instalação e Execução

1. Compile o projeto completo (na raiz):
   mvn clean install

2. Execute a API:
   Navegue até o módulo logistics-api e execute a classe LogisticsApplication.java ou use o comando via terminal:
   mvn spring-boot:run -pl logistics-api

3. Acesso:
   A API estará disponível em: http://localhost:8080/api/route

## Uso da API

Endpoint: GET /api/route

Parâmetros:
* from: ID do nó de origem
* to: ID do nó de destino

Exemplo:
GET http://localhost:8080/api/route?from=1&to=500

## Autor

Leonardo - Acadêmico de Engenharia de Software (UDESC)