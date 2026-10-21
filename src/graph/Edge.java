package graph;

/** Weighted edge (u -> v). For undirected graphs the adjacency list stores both directions. */
public final class Edge {
    public final int u, v, w;

    public Edge(int u, int v, int w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }

    @Override
    public String toString() {
        return u + "-" + v + "(" + w + ")";
    }
}
