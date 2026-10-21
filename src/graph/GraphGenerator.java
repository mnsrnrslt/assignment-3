package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Reproducible graph generators (fixed seed => identical graph on every run). */
public final class GraphGenerator {
    private GraphGenerator() {}

    private static int[] shuffled(int n, Random r) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        return p;
    }

    /**
     * Connected undirected graph with exactly m edges and no parallel edges / self-loops.
     * Step 1: random spanning tree (n-1 edges) guarantees connectivity.
     * Step 2: random extra edges until m is reached.
     */
    public static Graph connectedUndirected(int n, int m, long seed, int maxW) {
        if (m < n - 1 || (long) m > (long) n * (n - 1) / 2) throw new IllegalArgumentException("bad m");
        Random r = new Random(seed);
        Graph g = new Graph(n, false);
        Set<Long> used = new HashSet<>();
        int[] p = shuffled(n, r);
        for (int i = 1; i < n; i++) {
            int u = p[i], v = p[r.nextInt(i)];
            used.add((long) Math.min(u, v) * n + Math.max(u, v));
            g.addEdge(u, v, 1 + r.nextInt(maxW));
        }
        while (g.edgeCount() < m) {
            int u = r.nextInt(n), v = r.nextInt(n);
            if (u == v) continue;
            if (!used.add((long) Math.min(u, v) * n + Math.max(u, v))) continue;
            g.addEdge(u, v, 1 + r.nextInt(maxW));
        }
        return g;
    }

    /** Random directed graph (cycles are possible): m distinct directed edges, no self-loops. */
    public static Graph directedRandom(int n, int m, long seed, int maxW) {
        if ((long) m > (long) n * (n - 1)) throw new IllegalArgumentException("bad m");
        Random r = new Random(seed);
        Graph g = new Graph(n, true);
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < m) {
            int u = r.nextInt(n), v = r.nextInt(n);
            if (u == v || !used.add((long) u * n + v)) continue;
            g.addEdge(u, v, 1 + r.nextInt(maxW));
        }
        return g;
    }

    /** Random DAG: a random hidden order is drawn and every edge points forward in that order. */
    public static Graph dag(int n, int m, long seed, int maxW) {
        if ((long) m > (long) n * (n - 1) / 2) throw new IllegalArgumentException("bad m");
        Random r = new Random(seed);
        Graph g = new Graph(n, true);
        int[] pos = shuffled(n, r); // pos[v] = rank of v in hidden topological order
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < m) {
            int a = r.nextInt(n), b = r.nextInt(n);
            if (a == b) continue;
            int u = pos[a] < pos[b] ? a : b, v = u == a ? b : a;
            if (!used.add((long) u * n + v)) continue;
            g.addEdge(u, v, 1 + r.nextInt(maxW));
        }
        return g;
    }
}
