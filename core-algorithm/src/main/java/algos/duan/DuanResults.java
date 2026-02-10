package algos.duan;

import it.unimi.dsi.fastutil.ints.IntSet;

public class DuanResults {
    
    public static class BmsspResult {
        public final double newBound;
        public final IntSet U;
        public final IntSet activeFrontier; // <--- NOVO: Nós vistos mas não finalizados
        
        public BmsspResult(double newBound, IntSet U, IntSet activeFrontier) {
            this.newBound = newBound;
            this.U = U;
            this.activeFrontier = activeFrontier;
        }
    }

    public static class PivotsResult {
        public final IntSet P;
        public final IntSet W;
        
        public PivotsResult(IntSet P, IntSet W) {
            this.P = P;
            this.W = W;
        }
    }
}