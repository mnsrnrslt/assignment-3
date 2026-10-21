package mst;

import graph.Edge;
import graph.Graph;
import java.util.PriorityQueue;

/**
 * Prim with a binary-heap priority queue and lazy deletion (stale entries are skipped on poll).
 * Restarted from every unvisited vertex, so a disconnected graph yields a minimum spanning forest.
 */
public final class Prim {
    private Prim() {}

    public static MSTResult run(Graph g) {
        MSTResult res = new MSTResult("Prim");
        long[] cmp = {0};
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> { cmp[0]++; return Integer.compare(a[0], b[0]); });
        boolean[] visited = new boolean[g.n];
        int comps = 0;
        for (int s = 0; s < g.n; s++) {
            if (visited[s]) continue;
            comps++;
            visited[s] = true;
            push(g, s, visited, pq, res);
            while (!pq.isEmpty()) {
                int[] top = pq.poll(); // {weight, vertex, from}
                res.pqPops++;
                int v = top[1];
                if (visited[v]) continue;
                visited[v] = true;
                res.totalWeight += top[0];
                res.edges.add(new Edge(top[2], v, top[0]));
                push(g, v, visited, pq, res);
            }
        }
        res.comparisons = cmp[0];
        res.components = comps;
        res.connected = comps == 1;
        return res;
    }

    private static void push(Graph g, int u, boolean[] visited, PriorityQueue<int[]> pq, MSTResult res) {
        for (Edge e : g.adj(u)) {
            res.edgeExaminations++;
            if (!visited[e.v]) {
                pq.add(new int[]{e.w, e.v, u});
                res.pqPushes++;
            }
        }
    }
}
