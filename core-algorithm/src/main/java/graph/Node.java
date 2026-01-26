package graph;

public class Node {
    public final int id;
    public final double latitude;
    public final double longitude;

    // DIMACS geralmente fornece coordenadas como inteiros longos.
    // Ex: 40000000 -> Dividimos por 1 milhão para ter 40.0
    public Node(int id, long rawLat, long rawLon) {
        this.id = id;
        this.latitude = rawLat / 1_000_000.0;
        this.longitude = rawLon / 1_000_000.0;
    }
}