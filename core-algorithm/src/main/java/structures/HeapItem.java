package structures;

public class HeapItem implements Comparable<HeapItem> {
    public final int nodeId;
    public double distance; // Valor usado para ordenação

    public HeapItem(int nodeId, double distance) {
        this.nodeId = nodeId;
        this.distance = distance;
    }

    @Override
    public int compareTo(HeapItem other) {
        return Double.compare(this.distance, other.distance);
    }
}