package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Tarjan's strongly connected components (single DFS, low-link values, explicit vertex stack). */
public final class SCC {
    private final Graph g;
    private final int[] index, low;
    private final boolean[] onStack;
    private final Deque<Integer> stack = new ArrayDeque<>();
    private int counter = 0;
    private final Result res = new Result();

    public static final class Result {
        public final List<List<Integer>> components = new ArrayList<>();
        public long edgeExaminations;

        public int largest() {
            int m = 0;
            for (List<Integer> c : components) m = Math.max(m, c.size());
            return m;
        }
    }

    private SCC(Graph g) {
        this.g = g;
        index = new int[g.n];
        low = new int[g.n];
        onStack = new boolean[g.n];
        java.util.Arrays.fill(index, -1);
    }

    public static Result run(Graph g) {
        SCC s = new SCC(g);
        for (int v = 0; v < g.n; v++) if (s.index[v] == -1) s.dfs(v);
        return s.res;
    }

    private void dfs(int u) {
        index[u] = low[u] = counter++;
        stack.push(u);
        onStack[u] = true;
        for (Edge e : g.adj(u)) {
            res.edgeExaminations++;
            int v = e.v;
            if (index[v] == -1) {
                dfs(v);
                low[u] = Math.min(low[u], low[v]);
            } else if (onStack[v]) {
                low[u] = Math.min(low[u], index[v]);
            }
        }
        if (low[u] == index[u]) { // u is the root of an SCC: pop it off the stack
            List<Integer> comp = new ArrayList<>();
            int w;
            do {
                w = stack.pop();
                onStack[w] = false;
                comp.add(w);
            } while (w != u);
            res.components.add(comp);
        }
    }
}
