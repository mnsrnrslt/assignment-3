package shortestpath;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** Dijkstra with a binary heap and lazy deletion. Rejects negative weights. */
public final class Dijkstra {
    private Dijkstra() {}

    public static final long INF = Long.MAX_VALUE;

    public static final class Result {
        public int source;
        public long[] dist;
        public int[] prev;
        public long edgeRelaxationsTried, successfulRelaxations, pqPushes, pqPops, comparisons;

        public boolean reachable(int v) {
            return dist[v] != INF;
        }

        public int reachableCount() {
            int c = 0;
            for (long d : dist) if (d != INF) c++;
            return c;
        }

        /** Vertices on the shortest path source -> target, or empty list if target is unreachable. */
        public List<Integer> pathTo(int target) {
            List<Integer> path = new ArrayList<>();
            if (dist[target] == INF) return path;
            for (int v = target; v != -1; v = prev[v]) path.add(v);
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph g, int source) {
        for (Edge e : g.edges)
            if (e.w < 0) throw new IllegalArgumentException("Dijkstra requires non-negative weights, found " + e);
        Result res = new Result();
        res.source = source;
        res.dist = new long[g.n];
        res.prev = new int[g.n];
        Arrays.fill(res.dist, INF);
        Arrays.fill(res.prev, -1);
        long[] cmp = {0};
        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> { cmp[0]++; return Long.compare(a[0], b[0]); });
        res.dist[source] = 0;
        pq.add(new long[]{0, source});
        res.pqPushes++;
        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            res.pqPops++;
            int u = (int) top[1];
            if (top[0] > res.dist[u]) continue; // stale entry
            for (Edge e : g.adj(u)) {
                res.edgeRelaxationsTried++;
                long nd = top[0] + e.w;
                if (nd < res.dist[e.v]) {
                    res.dist[e.v] = nd;
                    res.prev[e.v] = u;
                    pq.add(new long[]{nd, e.v});
                    res.pqPushes++;
                    res.successfulRelaxations++;
                }
            }
        }
        res.comparisons = cmp[0];
        return res;
    }
}
