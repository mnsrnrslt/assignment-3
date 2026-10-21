package mst;

import graph.Edge;
import java.util.ArrayList;
import java.util.List;

/** Result + operation counters of an MST / minimum-spanning-forest run. */
public class MSTResult {
    public final String algorithm;
    public long totalWeight;
    public final List<Edge> edges = new ArrayList<>();
    public boolean connected;
    public int components;
    public long edgeExaminations, comparisons;
    public long dsuFinds, dsuUnions, dsuParentSteps; // Kruskal only
    public long pqPushes, pqPops;                    // Prim only

    public MSTResult(String algorithm) {
        this.algorithm = algorithm;
    }
}
