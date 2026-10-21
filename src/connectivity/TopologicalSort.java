package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Kahn's algorithm (in-degree + queue). Detects cycles: if not all vertices are output, a cycle exists. */
public final class TopologicalSort {
    private TopologicalSort() {}

    public static final class Result {
        public boolean isDag;
        /** Full topological order if isDag, otherwise only the vertices that could be ordered. */
        public final List<Integer> order = new ArrayList<>();
        public long edgeExaminations;
    }

    public static Result run(Graph g) {
        Result res = new Result();
        int[] indeg = new int[g.n];
        for (Edge e : g.edges) { indeg[e.v]++; res.edgeExaminations++; }
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int v = 0; v < g.n; v++) if (indeg[v] == 0) q.add(v);
        while (!q.isEmpty()) {
            int u = q.poll();
            res.order.add(u);
            for (Edge e : g.adj(u)) {
                res.edgeExaminations++;
                if (--indeg[e.v] == 0) q.add(e.v);
            }
        }
        res.isDag = res.order.size() == g.n;
        return res;
    }
}
