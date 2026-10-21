package app;

import connectivity.SCC;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.util.*;

/** Plain-Java test runner (no external libraries). Exit code 1 if any check fails. */
public class Tests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
    }

    static Graph undirected(int n, int[][] es) {
        Graph g = new Graph(n, false);
        for (int[] e : es) g.addEdge(e[0], e[1], e[2]);
        return g;
    }

    static Graph directed(int n, int[][] es) {
        Graph g = new Graph(n, true);
        for (int[] e : es) g.addEdge(e[0], e[1], e.length > 2 ? e[2] : 1);
        return g;
    }

    static boolean validTopo(Graph g, List<Integer> order) {
        if (order.size() != g.n) return false;
        int[] pos = new int[g.n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < order.size(); i++) {
            if (pos[order.get(i)] != -1) return false; // duplicate vertex
            pos[order.get(i)] = i;
        }
        for (Edge e : g.edges) if (pos[e.u] >= pos[e.v]) return false;
        return true;
    }

    static boolean[][] reachability(Graph g) {
        boolean[][] r = new boolean[g.n][g.n];
        for (int i = 0; i < g.n; i++) r[i][i] = true;
        for (Edge e : g.edges) r[e.u][e.v] = true;
        for (int k = 0; k < g.n; k++)
            for (int i = 0; i < g.n; i++)
                for (int j = 0; j < g.n; j++)
                    if (r[i][k] && r[k][j]) r[i][j] = true;
        return r;
    }

    static long[] bellmanFord(Graph g, int s) {
        long[] d = new long[g.n];
        Arrays.fill(d, Dijkstra.INF);
        d[s] = 0;
        for (int i = 0; i < g.n - 1; i++)
            for (Edge e : g.edges) {
                if (d[e.u] != Dijkstra.INF && d[e.u] + e.w < d[e.v]) d[e.v] = d[e.u] + e.w;
                if (!g.directed && d[e.v] != Dijkstra.INF && d[e.v] + e.w < d[e.u]) d[e.u] = d[e.v] + e.w;
            }
        return d;
    }

    public static void main(String[] args) {
        // ---------- DSU ----------
        boolean[][] variants = {{false, false}, {true, false}, {false, true}, {true, true}};
        boolean dsuOk = true;
        Random rnd = new Random(1);
        for (boolean[] v : variants) {
            DSU d = new DSU(50, v[0], v[1]);
            int[] label = new int[50];
            for (int i = 0; i < 50; i++) label[i] = i;
            for (int k = 0; k < 200; k++) {
                int a = rnd.nextInt(50), b = rnd.nextInt(50);
                boolean merged = d.union(a, b);
                boolean expected = label[a] != label[b];
                if (expected) { int old = label[b], nw = label[a]; for (int i = 0; i < 50; i++) if (label[i] == old) label[i] = nw; }
                if (merged != expected) dsuOk = false;
            }
            for (int a = 0; a < 50; a++) for (int b = 0; b < 50; b++) if (d.connected(a, b) != (label[a] == label[b])) dsuOk = false;
        }
        check("DSU: all 4 variants match a naive labelling model", dsuOk);
        DSU d0 = new DSU(5);
        d0.union(0, 1); d0.union(1, 2);
        check("DSU: components count after unions", d0.components() == 3 && d0.connected(0, 2) && !d0.connected(0, 4));

        // ---------- MST ----------
        Graph classic = undirected(5, new int[][]{{0, 1, 2}, {0, 3, 6}, {1, 2, 3}, {1, 3, 8}, {1, 4, 5}, {2, 4, 7}, {3, 4, 9}});
        MSTResult k = Kruskal.run(classic), p = Prim.run(classic);
        check("MST small graph: Kruskal weight = 16", k.totalWeight == 16 && k.connected && k.edges.size() == 4);
        check("MST small graph: Prim weight = 16", p.totalWeight == 16 && p.connected && p.edges.size() == 4);

        Graph single = new Graph(1, false);
        check("MST single vertex: weight 0, connected, no edges",
                Kruskal.run(single).totalWeight == 0 && Kruskal.run(single).connected && Prim.run(single).totalWeight == 0 && Prim.run(single).connected);

        Graph disc = undirected(6, new int[][]{{0, 1, 1}, {1, 2, 2}, {0, 2, 3}, {3, 4, 4}, {4, 5, 5}, {3, 5, 6}});
        MSTResult kd = Kruskal.run(disc), pd = Prim.run(disc);
        check("MST disconnected graph: detected (Kruskal & Prim), 2 components", !kd.connected && !pd.connected && kd.components == 2 && pd.components == 2);
        check("MST disconnected graph: forest weights equal (12)", kd.totalWeight == 12 && pd.totalWeight == 12);

        Graph square = undirected(4, new int[][]{{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {3, 0, 1}});
        check("MST multiple valid MSTs (4-cycle, equal weights): same weight 3", Kruskal.run(square).totalWeight == 3 && Prim.run(square).totalWeight == 3);
        Graph k4 = undirected(4, new int[][]{{0, 1, 5}, {0, 2, 5}, {0, 3, 5}, {1, 2, 5}, {1, 3, 5}, {2, 3, 5}});
        check("MST duplicate weights (K4, all equal): same weight 15", Kruskal.run(k4).totalWeight == 15 && Prim.run(k4).totalWeight == 15);

        boolean agree = true;
        for (int t = 0; t < 200; t++) {
            int n = 2 + rnd.nextInt(30);
            int maxE = n * (n - 1) / 2;
            int m = Math.min(maxE, n - 1 + rnd.nextInt(2 * n));
            Graph g = GraphGenerator.connectedUndirected(n, m, 1000 + t, 1 + rnd.nextInt(10)); // small weight range => many duplicates
            MSTResult a = Kruskal.run(g), b = Prim.run(g);
            if (!a.connected || !b.connected || a.totalWeight != b.totalWeight || a.edges.size() != n - 1 || b.edges.size() != n - 1) agree = false;
        }
        check("MST: Kruskal == Prim on 200 random connected graphs with duplicate weights", agree);

        // ---------- Topological sort ----------
        Graph dag = directed(6, new int[][]{{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}});
        TopologicalSort.Result tr = TopologicalSort.run(dag);
        check("Topo: small DAG gives a valid order", tr.isDag && validTopo(dag, tr.order));
        Graph cyc = directed(4, new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 3}});
        check("Topo: cycle detected (no ordering returned)", !TopologicalSort.run(cyc).isDag);
        check("Topo: single vertex", TopologicalSort.run(new Graph(1, true)).isDag);
        boolean topoOk = true;
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(40);
            Graph g = GraphGenerator.dag(n, Math.min(n * (n - 1) / 2, n + rnd.nextInt(3 * n)), 2000 + t, 5);
            TopologicalSort.Result r = TopologicalSort.run(g);
            if (!r.isDag || !validTopo(g, r.order)) topoOk = false;
        }
        check("Topo: valid order for 100 random DAGs", topoOk);

        // ---------- SCC ----------
        Graph sccG = directed(8, new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 5}, {5, 3}, {6, 5}, {6, 7}, {7, 6}});
        SCC.Result sr = SCC.run(sccG);
        check("SCC: known example has 3 components ({0,1,2},{3,4,5},{6,7})", sr.components.size() == 3);
        check("SCC: single vertex = 1 component", SCC.run(new Graph(1, true)).components.size() == 1);
        boolean sccOk = true, cross = true;
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(25);
            Graph g = GraphGenerator.directedRandom(n, Math.min(n * (n - 1), rnd.nextInt(3 * n) + 1), 3000 + t, 5);
            SCC.Result r = SCC.run(g);
            int[] comp = new int[n];
            Arrays.fill(comp, -1);
            int total = 0;
            for (int c = 0; c < r.components.size(); c++)
                for (int v : r.components.get(c)) { if (comp[v] != -1) sccOk = false; comp[v] = c; total++; }
            if (total != n) sccOk = false; // every vertex in exactly one SCC
            boolean[][] reach = reachability(g);
            for (int a = 0; a < n; a++) for (int b = 0; b < n; b++)
                if ((comp[a] == comp[b]) != (reach[a][b] && reach[b][a])) sccOk = false;
            if (TopologicalSort.run(g).isDag != (r.largest() == 1)) cross = false; // DAG <=> all SCCs trivial
        }
        check("SCC: every vertex in exactly one SCC and matches Floyd-Warshall mutual reachability (100 random graphs)", sccOk);
        check("SCC vs Topo: graph is a DAG iff all SCCs have size 1 (100 random graphs)", cross);

        // ---------- Dijkstra ----------
        Graph dj = directed(5, new int[][]{{0, 1, 10}, {0, 2, 3}, {2, 1, 4}, {1, 3, 2}, {2, 3, 8}});
        Dijkstra.Result dr = Dijkstra.run(dj, 0);
        check("Dijkstra: known distances {0,7,3,9} and unreachable vertex 4 = INF",
                dr.dist[0] == 0 && dr.dist[1] == 7 && dr.dist[2] == 3 && dr.dist[3] == 9 && dr.dist[4] == Dijkstra.INF && !dr.reachable(4));
        check("Dijkstra: path 0->3 is [0,2,1,3], path to unreachable is empty",
                dr.pathTo(3).equals(Arrays.asList(0, 2, 1, 3)) && dr.pathTo(4).isEmpty());
        check("Dijkstra: single vertex", Dijkstra.run(new Graph(1, false), 0).dist[0] == 0);
        boolean neg = false;
        try { Dijkstra.run(directed(3, new int[][]{{0, 1, 4}, {0, 2, 5}, {2, 1, -3}}), 0); } catch (IllegalArgumentException e) { neg = true; }
        check("Dijkstra: negative edge weight is rejected", neg);
        boolean djOk = true;
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(30);
            boolean dirG = t % 2 == 0;
            Graph g = dirG ? GraphGenerator.directedRandom(n, Math.min(n * (n - 1), rnd.nextInt(4 * n) + 1), 4000 + t, 20)
                           : GraphGenerator.connectedUndirected(n, Math.min(n * (n - 1) / 2, n - 1 + rnd.nextInt(3 * n)), 4000 + t, 20);
            Dijkstra.Result r = Dijkstra.run(g, 0);
            if (!Arrays.equals(r.dist, bellmanFord(g, 0))) djOk = false;
            for (int v = 0; v < n; v++) { // path consistency: path exists iff reachable
                List<Integer> path = r.pathTo(v);
                if (r.reachable(v) != !path.isEmpty()) djOk = false;
            }
        }
        check("Dijkstra: matches Bellman-Ford on 100 random graphs (directed and undirected, with unreachable vertices)", djOk);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
