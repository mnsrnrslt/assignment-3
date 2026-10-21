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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Runs all experiments of Assignment 3 and writes results/tables/tables.md and results/tables/timing.csv.
 * Usage: java -cp out app.Benchmark [resultsDir]
 */
public class Benchmark {
    static final long SEED = 42;
    static final int REPEATS = 5, WARMUP = 100, MAXW = 100;
    static final String[] NAMES = {"Small", "Medium", "Large"};
    static final int[] VS = {10, 100, 1000}, ES = {30, 500, 5000};

    static final StringBuilder csv = new StringBuilder("set,algorithm,label,V,E,avg_us,ops\n");
    static final StringBuilder md = new StringBuilder();

    static final class Timed<T> {
        double avgNs;
        T last;
    }

    /** WARMUP untimed runs (JIT), then REPEATS timed runs; returns the mean of System.nanoTime() deltas. */
    static <T> Timed<T> time(Supplier<T> s) {
        for (int i = 0; i < WARMUP; i++) s.get();
        System.gc(); // collect warm-up garbage outside the timed region
        long total = 0;
        T last = null;
        for (int i = 0; i < REPEATS; i++) {
            long t0 = System.nanoTime();
            last = s.get();
            total += System.nanoTime() - t0;
        }
        Timed<T> r = new Timed<>();
        r.avgNs = total / (double) REPEATS;
        r.last = last;
        return r;
    }

    static String us(double ns) {
        return String.format(Locale.US, "%.2f", ns / 1000.0);
    }

    static void rec(String set, String algo, String label, int v, int e, double ns, long ops) {
        csv.append(String.format(Locale.US, "%s,%s,%s,%d,%d,%.3f,%d\n", set, algo, label, v, e, ns / 1000.0, ops));
    }

    static final class Table {
        final StringBuilder sb = new StringBuilder();

        Table(String... h) {
            row((Object[]) h);
            StringBuilder sep = new StringBuilder("|");
            for (int i = 0; i < h.length; i++) sep.append("---|");
            sb.append(sep).append("\n");
        }

        void row(Object... c) {
            sb.append("|");
            for (Object o : c) sb.append(" ").append(o).append(" |");
            sb.append("\n");
        }

        void emit(String title) {
            md.append("### ").append(title).append("\n\n").append(sb).append("\n");
        }
    }

    static boolean validTopo(Graph g, List<Integer> order) {
        if (order.size() != g.n) return false;
        int[] pos = new int[g.n];
        for (int i = 0; i < g.n; i++) pos[order.get(i)] = i;
        for (Edge e : g.edges) if (pos[e.u] >= pos[e.v]) return false;
        return true;
    }

    static DSU chain(int n, boolean c, boolean r) {
        DSU d = new DSU(n, c, r);
        for (int i = 0; i < n - 1; i++) d.union(i + 1, i); // naive linking builds one long chain
        for (int q = 0; q < n; q++) d.find(0);              // repeated queries from the deepest node
        return d;
    }

    public static void main(String[] args) throws Exception {
        Path out = Paths.get(args.length > 0 ? args[0] : "results");
        Thread t = new Thread(null, () -> {
            try { runAll(out); } catch (Exception e) { throw new RuntimeException(e); }
        }, "bench", 1L << 28);
        t.start();
        t.join();
    }

    static void runAll(Path out) throws Exception {
        md.append("_Environment: Java ").append(System.getProperty("java.version")).append(", ")
          .append(System.getProperty("os.name")).append(", ").append(Runtime.getRuntime().availableProcessors())
          .append(" logical CPUs. Seed = ").append(SEED).append(", ").append(WARMUP).append(" warm-up runs, mean of ")
          .append(REPEATS).append(" timed runs (System.nanoTime), edge weights uniform in [1, ").append(MAXW).append("]._\n\n");

        Table mstT = new Table("Graph", "V", "E", "Algorithm", "Avg time (µs)", "MST weight", "Edge exams", "Comparisons", "DSU finds / unions", "PQ pushes / pops", "Connected");
        Table dsuT = new Table("Graph", "DSU variant", "Avg Kruskal time (µs)", "find calls", "union calls", "parent-pointer hops");
        Table topoT = new Table("Graph", "V", "E", "Input type", "Is DAG?", "Vertices ordered", "Order check", "Edge exams", "Avg time (µs)");
        Table sccT = new Table("Graph", "V", "E", "Input type", "# SCCs", "Largest SCC", "Edge exams", "Avg time (µs)");
        Table djT = new Table("Graph", "V", "E", "Avg time (µs)", "Reachable", "Relaxations tried", "Successful relaxations", "PQ pushes / pops", "Comparisons", "Max distance");
        boolean allEqual = true;
        String[] vn = {"none (naive)", "path compression only", "union by rank only", "both"};
        boolean[][] vv = {{false, false}, {true, false}, {false, true}, {true, true}};

        for (int i = 0; i < 3; i++) {
            int V = VS[i], E = ES[i];
            Graph ug = GraphGenerator.connectedUndirected(V, E, SEED, MAXW);
            Graph dag = GraphGenerator.dag(V, E, SEED, MAXW);
            Graph cyc = GraphGenerator.directedRandom(V, E, SEED + 1, MAXW);

            // ---- MST
            Timed<MSTResult> k = time(() -> Kruskal.run(ug)), p = time(() -> Prim.run(ug));
            allEqual &= k.last.totalWeight == p.last.totalWeight;
            mstT.row(NAMES[i], V, E, "Kruskal", us(k.avgNs), k.last.totalWeight, k.last.edgeExaminations, k.last.comparisons, k.last.dsuFinds + " / " + k.last.dsuUnions, "-", k.last.connected);
            mstT.row(NAMES[i], V, E, "Prim", us(p.avgNs), p.last.totalWeight, p.last.edgeExaminations, p.last.comparisons, "-", p.last.pqPushes + " / " + p.last.pqPops, p.last.connected);
            rec("required", "Kruskal", NAMES[i], V, E, k.avgNs, k.last.edgeExaminations);
            rec("required", "Prim", NAMES[i], V, E, p.avgNs, p.last.edgeExaminations);

            // ---- DSU variants inside Kruskal
            for (int j = 0; j < 4; j++) {
                final boolean c = vv[j][0], r = vv[j][1];
                Timed<MSTResult> kv = time(() -> Kruskal.run(ug, c, r));
                dsuT.row(NAMES[i], vn[j], us(kv.avgNs), kv.last.dsuFinds, kv.last.dsuUnions, kv.last.dsuParentSteps);
            }

            // ---- Topological ordering: DAG and cyclic input
            Timed<TopologicalSort.Result> td = time(() -> TopologicalSort.run(dag)), tc = time(() -> TopologicalSort.run(cyc));
            topoT.row(NAMES[i], V, E, "random DAG", td.last.isDag, td.last.order.size() + " / " + V, validTopo(dag, td.last.order) ? "valid" : "INVALID", td.last.edgeExaminations, us(td.avgNs));
            topoT.row(NAMES[i], V, E, "random directed (has cycles)", tc.last.isDag, tc.last.order.size() + " / " + V, "cycle reported", tc.last.edgeExaminations, us(tc.avgNs));
            rec("required", "Topo (DAG)", NAMES[i], V, E, td.avgNs, td.last.edgeExaminations);
            rec("required", "Topo (cyclic)", NAMES[i], V, E, tc.avgNs, tc.last.edgeExaminations);

            // ---- SCC
            Timed<SCC.Result> sc = time(() -> SCC.run(cyc)), sd = time(() -> SCC.run(dag));
            sccT.row(NAMES[i], V, E, "random directed (has cycles)", sc.last.components.size(), sc.last.largest(), sc.last.edgeExaminations, us(sc.avgNs));
            sccT.row(NAMES[i], V, E, "random DAG", sd.last.components.size(), sd.last.largest(), sd.last.edgeExaminations, us(sd.avgNs));
            rec("required", "SCC (Tarjan)", NAMES[i], V, E, sc.avgNs, sc.last.edgeExaminations);

            // ---- Dijkstra (undirected connected "road" graph, source 0)
            Timed<Dijkstra.Result> dj = time(() -> Dijkstra.run(ug, 0));
            long maxD = 0;
            for (long d : dj.last.dist) if (d != Dijkstra.INF) maxD = Math.max(maxD, d);
            djT.row(NAMES[i], V, E, us(dj.avgNs), dj.last.reachableCount() + " / " + V, dj.last.edgeRelaxationsTried, dj.last.successfulRelaxations, dj.last.pqPushes + " / " + dj.last.pqPops, dj.last.comparisons, maxD);
            rec("required", "Dijkstra", NAMES[i], V, E, dj.avgNs, dj.last.edgeRelaxationsTried);
        }

        md.append("## 1. MST: Kruskal vs Prim\n\n");
        mstT.emit("MST results (connected undirected graphs)");
        md.append("Kruskal and Prim returned the same MST weight on all three graphs: **").append(allEqual ? "yes" : "NO").append("**.\n\n");
        md.append("## 2. DSU optimisations\n\n");
        dsuT.emit("DSU variants used inside Kruskal (same graphs as above)");

        // adversarial micro-benchmark
        int N = 5000;
        Table adv = new Table("DSU variant", "Avg time (µs)", "find calls", "parent-pointer hops");
        for (int j = 0; j < 4; j++) {
            final boolean c = vv[j][0], r = vv[j][1];
            Timed<DSU> d = time(() -> chain(N, c, r));
            adv.row(vn[j], us(d.avgNs), d.last.findCalls, d.last.parentSteps);
        }
        adv.emit("DSU worst-case micro-benchmark (n = " + N + ": build a chain with union(i+1, i), then n x find(0))");

        md.append("## 3. Topological ordering\n\n");
        topoT.emit("Topological ordering on DAGs and on graphs with cycles");
        md.append("## 4. Strongly connected components\n\n");
        sccT.emit("SCC (Tarjan)");
        md.append("## 5. Dijkstra\n\n");
        djT.emit("Dijkstra from source 0 on the connected undirected graphs");

        // ---- sweeps for the scaling plots
        md.append("## 6. Scaling sweeps (used for the extra plots)\n\n");
        int[] sv = {100, 200, 400, 800, 1600, 3200};
        Table swV = new Table("V", "E", "Kruskal (µs)", "Prim (µs)", "Dijkstra (µs)", "SCC (µs)", "Topo DAG (µs)", "Topo cyclic (µs)");
        for (int v : sv) sweepPoint("sweepV", v, 5 * v, swV);
        swV.emit("Sweep A: E = 5V, V grows");
        int[] se = {2000, 4000, 8000, 16000, 32000, 64000};
        Table swE = new Table("V", "E", "Kruskal (µs)", "Prim (µs)", "Dijkstra (µs)", "SCC (µs)", "Topo DAG (µs)", "Topo cyclic (µs)");
        for (int e : se) sweepPoint("sweepE", 1000, e, swE);
        swE.emit("Sweep B: V = 1000, E grows");

        Files.createDirectories(out.resolve("tables"));
        Files.writeString(out.resolve("tables/tables.md"), md.toString());
        Files.writeString(out.resolve("tables/timing.csv"), csv.toString());
        System.out.println(md);
        System.out.println("Written to " + out.resolve("tables").toAbsolutePath());
    }

    static void sweepPoint(String set, int V, int E, Table tab) {
        Graph ug = GraphGenerator.connectedUndirected(V, E, SEED, MAXW);
        Graph dg = GraphGenerator.directedRandom(V, E, SEED + 1, MAXW);
        Graph dag = GraphGenerator.dag(V, E, SEED, MAXW);
        Timed<MSTResult> k = time(() -> Kruskal.run(ug)), p = time(() -> Prim.run(ug));
        Timed<Dijkstra.Result> d = time(() -> Dijkstra.run(ug, 0));
        Timed<SCC.Result> s = time(() -> SCC.run(dg));
        Timed<TopologicalSort.Result> td = time(() -> TopologicalSort.run(dag)), tc = time(() -> TopologicalSort.run(dg));
        String lab = set.equals("sweepV") ? "V=" + V : "E=" + E;
        rec(set, "Kruskal", lab, V, E, k.avgNs, k.last.edgeExaminations);
        rec(set, "Prim", lab, V, E, p.avgNs, p.last.edgeExaminations);
        rec(set, "Dijkstra", lab, V, E, d.avgNs, d.last.edgeRelaxationsTried);
        rec(set, "SCC (Tarjan)", lab, V, E, s.avgNs, s.last.edgeExaminations);
        rec(set, "Topo (DAG)", lab, V, E, td.avgNs, td.last.edgeExaminations);
        rec(set, "Topo (cyclic)", lab, V, E, tc.avgNs, tc.last.edgeExaminations);
        tab.row(V, E, us(k.avgNs), us(p.avgNs), us(d.avgNs), us(s.avgNs), us(td.avgNs), us(tc.avgNs));
    }
}
