package mst;

import dsu.DSU;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;

/** Kruskal: sort edges by weight, add an edge if it joins two different DSU sets. */
public final class Kruskal {
    private Kruskal() {}

    public static MSTResult run(Graph g) {
        return run(g, true, true);
    }

    /** Disconnected input => minimum spanning forest and connected == false. */
    public static MSTResult run(Graph g, boolean pathCompression, boolean unionByRank) {
        MSTResult res = new MSTResult("Kruskal");
        long[] cmp = {0};
        List<Edge> sorted = new ArrayList<>(g.edges);
        sorted.sort((a, b) -> { cmp[0]++; return Integer.compare(a.w, b.w); });
        DSU dsu = new DSU(g.n, pathCompression, unionByRank);
        for (Edge e : sorted) {
            res.edgeExaminations++;
            if (dsu.union(e.u, e.v)) {
                res.edges.add(e);
                res.totalWeight += e.w;
                if (res.edges.size() == g.n - 1) break; // spanning tree complete
            }
        }
        res.comparisons = cmp[0];
        res.components = dsu.components();
        res.connected = res.components == 1;
        res.dsuFinds = dsu.findCalls;
        res.dsuUnions = dsu.unionCalls;
        res.dsuParentSteps = dsu.parentSteps;
        return res;
    }
}
