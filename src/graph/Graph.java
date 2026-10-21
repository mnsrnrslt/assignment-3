package graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph stored as an adjacency list plus a flat edge list.
 * Vertices are 0..n-1. For an undirected graph addEdge() stores the edge once in
 * {@link #edges} (used by Kruskal) and in both adjacency lists (used by Prim, DFS, Dijkstra).
 */
public class Graph {
    public final int n;
    public final boolean directed;
    public final List<Edge> edges = new ArrayList<>();
    private final List<List<Edge>> adj = new ArrayList<>();

    public Graph(int n, boolean directed) {
        this.n = n;
        this.directed = directed;
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int u, int v, int w) {
        Edge e = new Edge(u, v, w);
        edges.add(e);
        adj.get(u).add(e);
        if (!directed) adj.get(v).add(new Edge(v, u, w));
    }

    /** Outgoing edges of u; for each returned edge, e.u == u and e.v is the neighbour. */
    public List<Edge> adj(int u) {
        return adj.get(u);
    }

    public int edgeCount() {
        return edges.size();
    }
}
