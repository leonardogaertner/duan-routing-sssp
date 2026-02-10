package structures;

import java.util.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.doubles.*;

/**
 * Block Priority Queue Corrigida (Tight Bounds + Fat Block Protection).
 * Resolve a inversão de prioridade garantindo que as chaves dos blocos
 * reflitam o verdadeiro máximo dos seus elementos.
 */
public class BlockPriorityQueue implements DuanHeap {

    private static class Block {
        IntArrayList nodes;
        DoubleArrayList costs;
        double maxVal; 

        public Block(int capacity) {
            this.nodes = new IntArrayList(capacity);
            this.costs = new DoubleArrayList(capacity);
            this.maxVal = -1.0;
        }

        void add(int u, double w) {
            nodes.add(u);
            costs.add(w);
            if (w > maxVal) maxVal = w;
        }

        void merge(Block other) {
            this.nodes.addAll(other.nodes);
            this.costs.addAll(other.costs);
            if (other.maxVal > this.maxVal) this.maxVal = other.maxVal;
        }

        boolean isEmpty() { return nodes.isEmpty(); }
        int size() { return nodes.size(); }
        
        void sort() {
            int size = size();
            if (size <= 1) return;
            
            int[] perm = new int[size];
            for(int i=0; i<size; i++) perm[i] = i;
            
            final double[] c = costs.elements();
            IntArrays.quickSort(perm, 0, size, (i, j) -> Double.compare(c[i], c[j]));
            
            int[] newNodes = new int[size];
            double[] newCosts = new double[size];
            for(int i=0; i<size; i++) {
                newNodes[i] = nodes.getInt(perm[i]);
                newCosts[i] = costs.getDouble(perm[i]);
            }
            nodes = IntArrayList.wrap(newNodes);
            costs = DoubleArrayList.wrap(newCosts);
        }
    }

    private long M;
    private double globalBound;
    private int size; 
    
    private ArrayDeque<Block> D0;
    private TreeMap<Double, Block> D1;
    private Int2DoubleOpenHashMap currentBest;

    public BlockPriorityQueue() {
        this.D0 = new ArrayDeque<>();
        this.D1 = new TreeMap<>();
        this.currentBest = new Int2DoubleOpenHashMap();
        this.currentBest.defaultReturnValue(Double.MAX_VALUE);
    }

    @Override
    public void initialize(long M, double B) {
        this.M = M;
        this.globalBound = B;
        this.size = 0;
        this.D0.clear();
        this.D1.clear();
        this.currentBest.clear();
        
        // Inicializa com um bloco sentinela
        Block initialBlock = new Block((int)Math.min(M, 100));
        initialBlock.maxVal = B;
        this.D1.put(B, initialBlock);
    }

    @Override
    public void insert(int u, double w) {
        double oldW = currentBest.get(u);
        if (w >= oldW) return;
        currentBest.put(u, w);
        size++;

        Map.Entry<Double, Block> entry = D1.ceilingEntry(w);
        Block targetBlock;
        double targetKey;

        if (entry == null) {
            // Se cair fora de qualquer bloco, cria um novo no topo
            targetKey = Double.MAX_VALUE;
            targetBlock = new Block(16);
            targetBlock.maxVal = targetKey;
            D1.put(targetKey, targetBlock);
        } else {
            targetKey = entry.getKey();
            targetBlock = entry.getValue();
        }

        targetBlock.add(u, w);

        if (targetBlock.size() > M) {
            splitBlock(targetKey, targetBlock);
        }
    }

    private void splitBlock(double oldKey, Block block) {
        D1.remove(oldKey); 
        block.sort();
        
        int total = block.size();
        int mid = total / 2;
        
        // Bloco 1: Metade inferior
        Block b1 = new Block(mid + 1);
        for(int i=0; i<mid; i++) b1.add(block.nodes.getInt(i), block.costs.getDouble(i));
        
        if (!b1.isEmpty()) {
            b1.maxVal = block.costs.getDouble(mid - 1);
        } else {
            b1.maxVal = oldKey; // Fallback raro
        }

        // Bloco 2: Metade superior
        Block b2 = new Block(total - mid + 1);
        for(int i=mid; i<total; i++) b2.add(block.nodes.getInt(i), block.costs.getDouble(i));
        
        // [CORREÇÃO CRÍTICA]: Tight Bounds
        // Definimos a chave de b2 baseada nos dados reais, não em oldKey.
        // Isso permite que b2 "pule" para frente na fila se seus valores forem pequenos.
        if (!b2.isEmpty()) {
            b2.maxVal = block.costs.getDouble(total - 1);
        } else {
            b2.maxVal = oldKey;
        }

        // [PROTEÇÃO]: Fat Block & Identical Keys
        // Se as chaves colidirem (ex: muitos valores iguais), o safePut fará o merge.
        if (b1.maxVal >= b2.maxVal) {
             // Caso especial: inversão ou igualdade devido a valores idênticos.
             // O safePut vai lidar com isso fundindo-os na chave maior.
             // Mas para garantir a ordem, b1 deve ir primeiro logicamente.
             // Como são iguais, a ordem no TreeMap é a mesma chave.
        }

        safePut(b1.maxVal, b1);
        safePut(b2.maxVal, b2);
    }

    private void safePut(double key, Block newBlock) {
        if (newBlock.isEmpty()) return;

        Block existing = D1.get(key);
        if (existing != null) {
            // Colisão de chave: Fundir (Fat Block Strategy)
            existing.merge(newBlock);
            
            // Só tentamos dividir novamente se cresceu muito E se não estamos num loop de chaves iguais.
            // Para evitar StackOverflow, verificamos se a divisão anterior foi produtiva.
            // Como simplificação segura: se fundimos, aceitamos o bloco gordo temporariamente.
            // Ele será drenado pelo pull de qualquer forma.
        } else {
            D1.put(key, newBlock);
        }
    }

    @Override
    public void batchPrepend(List<HeapItem> items) {
        if (items.isEmpty()) return;

        Block current = new Block((int)Math.min(items.size(), M));
        
        for (HeapItem item : items) {
            double oldW = currentBest.get(item.nodeId);
            if (item.distance >= oldW) continue;
            currentBest.put(item.nodeId, item.distance);
            size++;

            current.add(item.nodeId, item.distance);
            
            if (current.size() >= M) {
                D0.addFirst(current);
                current = new Block((int)M);
            }
        }
        
        if (!current.isEmpty()) {
            D0.addFirst(current);
        }
    }

    @Override
    public PullResult pull() {
        if (isEmpty()) return new PullResult(new ArrayList<>(), globalBound);

        List<HeapItem> candidates = new ArrayList<>();
        
        // 1. Drenar D0 (Buffer não ordenado)
        while (!D0.isEmpty()) {
            Block b = D0.pollFirst();
            for(int i=0; i<b.size(); i++) {
                candidates.add(new HeapItem(b.nodes.getInt(i), b.costs.getDouble(i)));
            }
        }

        // 2. Drenar D1 (Blocos ordenados)
        // Agora que as chaves são "Tight", a ordem do TreeMap reflete a realidade.
        int takenFromD1 = 0;
        Iterator<Map.Entry<Double, Block>> it = D1.entrySet().iterator();
        while (it.hasNext() && takenFromD1 < M) {
            Map.Entry<Double, Block> entry = it.next();
            Block b = entry.getValue();
            for(int i=0; i<b.size(); i++) {
                candidates.add(new HeapItem(b.nodes.getInt(i), b.costs.getDouble(i)));
            }
            takenFromD1 += b.size();
            it.remove(); 
        }

        if (candidates.size() <= M) {
            List<HeapItem> validItems = filterValid(candidates);
            
            // Reset total
            size = 0;
            D0.clear();
            D1.clear();
            Block sentinel = new Block(10); 
            sentinel.maxVal = globalBound;
            D1.put(globalBound, sentinel);
            
            return new PullResult(validItems, globalBound);
        }

        candidates.sort(Comparator.comparingDouble(item -> item.distance));

        List<HeapItem> returnSet = new ArrayList<>();
        List<HeapItem> leftOvers = new ArrayList<>();
        double newBound = globalBound;
        
        for (int i = 0; i < candidates.size(); i++) {
            HeapItem item = candidates.get(i);
            if (i < M) {
                returnSet.add(item);
            } else {
                if (i == M) newBound = item.distance;
                leftOvers.add(item);
            }
        }
        
        returnSet = filterValid(returnSet);
        size -= returnSet.size();

        // Reinsere sobras (vão encontrar seu lugar correto no TreeMap devido ao insert Tight)
        for (HeapItem item : leftOvers) {
            insert(item.nodeId, item.distance);
        }

        return new PullResult(returnSet, newBound);
    }

    private List<HeapItem> filterValid(List<HeapItem> items) {
        List<HeapItem> valid = new ArrayList<>(items.size());
        for (HeapItem item : items) {
            if (item.distance <= currentBest.get(item.nodeId)) {
                valid.add(item);
            }
        }
        return valid;
    }

    @Override
    public boolean isEmpty() {
        if (!D0.isEmpty()) return false;
        if (D1.isEmpty()) return true;
        return size == 0;
    }
    
    @Override
    public IntSet drain() {
        IntSet leftovers = new IntOpenHashSet();
        
        // Coleta de D0
        for (Block b : D0) {
            leftovers.addAll(b.nodes);
        }
        
        // Coleta de D1
        for (Block b : D1.values()) {
            leftovers.addAll(b.nodes);
        }
        
        // Limpa a estrutura
        D0.clear();
        D1.clear();
        size = 0;
        
        return leftovers;
    }
}