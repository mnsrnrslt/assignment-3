"""Reads results/tables/timing.csv and writes the plots into results/plots/."""
import csv
import os
import sys
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

root = sys.argv[1] if len(sys.argv) > 1 else "results"
rows = list(csv.DictReader(open(os.path.join(root, "tables", "timing.csv"), encoding="utf-8")))
os.makedirs(os.path.join(root, "plots"), exist_ok=True)

MARK = {"Kruskal": "o", "Prim": "s", "Dijkstra": "^", "SCC (Tarjan)": "D", "Topo (DAG)": "v", "Topo (cyclic)": "x"}


def plot(dataset, xkey, xlabel, title, fname, logx=False):
    series = defaultdict(list)
    for r in rows:
        if r["set"] == dataset:
            series[r["algorithm"]].append((int(r[xkey]), float(r["avg_us"])))
    plt.figure(figsize=(7.5, 4.8))
    for algo, pts in series.items():
        pts.sort()
        plt.plot([p[0] for p in pts], [p[1] for p in pts], marker=MARK.get(algo, "o"), label=algo)
    plt.xlabel(xlabel)
    plt.ylabel("Average execution time (µs)")
    plt.title(title)
    plt.yscale("log")
    if logx:
        plt.xscale("log")
    plt.grid(True, which="both", alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(root, "plots", fname), dpi=140)
    plt.close()


plot("required", "V", "Number of vertices V (Small / Medium / Large)", "Execution time vs. number of vertices (required graphs)", "time_vs_vertices.png", logx=True)
plot("required", "E", "Number of edges E (Small / Medium / Large)", "Execution time vs. number of edges (required graphs)", "time_vs_edges.png", logx=True)
plot("sweepV", "V", "Number of vertices V (E = 5V)", "Scaling sweep A: time vs. V", "sweep_time_vs_vertices.png", logx=True)
plot("sweepE", "E", "Number of edges E (V = 1000)", "Scaling sweep B: time vs. E", "sweep_time_vs_edges.png", logx=True)
print("plots written to", os.path.join(root, "plots"))
