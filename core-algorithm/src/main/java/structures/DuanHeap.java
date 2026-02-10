package structures;

import java.util.List;
import it.unimi.dsi.fastutil.ints.IntSet; // Import necessário

public interface DuanHeap {
    
    class PullResult {
        public final List<HeapItem> items;
        public final double newBound;
        
        public PullResult(List<HeapItem> items, double newBound) {
            this.items = items;
            this.newBound = newBound;
        }
    }

    void initialize(long M, double B);
    void insert(int u, double w);
    void batchPrepend(List<HeapItem> items);
    PullResult pull();
    boolean isEmpty();
    
    // NOVO MÉTODO
    IntSet drain(); 
}