package structures;

import java.util.List;

/**
 * Interface baseada no Lemma 3.3 do paper.
 * Suporta Insert, BatchPrepend e Pull.
 */
public interface DuanHeap {
    
    // Configura a estrutura para um novo nível de recursão (Parametro M e Bound B) [cite: 162]
    void initialize(long M, double maxBound);
    
    // Insere um item individual [cite: 147]
    void insert(int nodeId, double distance);
    
    // A "mágica" do paper: insere vários itens pequenos de uma vez [cite: 149]
    void batchPrepend(List<HeapItem> items);
    
    // Retorna os M menores itens e o novo bound [cite: 151]
    PullResult pull();
    
    boolean isEmpty();
    
    // Classe auxiliar para o retorno do Pull
    class PullResult {
        public final List<HeapItem> items; // S'
        public final double newBound;      // B_i
        
        public PullResult(List<HeapItem> items, double newBound) {
            this.items = items;
            this.newBound = newBound;
        }
    }
}