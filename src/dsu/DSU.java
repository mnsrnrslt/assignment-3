package dsu;

/**
 * Disjoint Set Union. Path compression and union by rank can be switched on/off independently
 * so that the experiments can measure what each optimisation contributes.
 * Counters: findCalls (includes the 2 finds done inside every union), unionCalls,
 * parentSteps (number of parent-pointer hops while searching for roots).
 */
public class DSU {
    private final int[] parent, rank;
    private final boolean compress, byRank;
    private int components;
    public long findCalls, unionCalls, parentSteps;

    public DSU(int n) {
        this(n, true, true);
    }

    public DSU(int n, boolean pathCompression, boolean unionByRank) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        components = n;
        compress = pathCompression;
        byRank = unionByRank;
    }

    public int find(int x) {
        findCalls++;
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
            parentSteps++;
        }
        if (compress) { // second pass: point every node on the path directly to the root
            while (parent[x] != root) {
                int next = parent[x];
                parent[x] = root;
                x = next;
            }
        }
        return root;
    }

    /** @return true if two different sets were merged, false if already in the same set. */
    public boolean union(int a, int b) {
        unionCalls++;
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (byRank) {
            if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            if (rank[ra] == rank[rb]) rank[ra]++;
        } else {
            parent[rb] = ra; // naive linking, can build long chains
        }
        components--;
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    public int components() {
        return components;
    }
}
