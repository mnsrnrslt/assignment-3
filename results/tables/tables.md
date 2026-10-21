_Environment: Java 21.0.10, Linux, 1 logical CPUs. Seed = 42, 100 warm-up runs, mean of 5 timed runs (System.nanoTime), edge weights uniform in [1, 100]._

## 1. MST: Kruskal vs Prim

### MST results (connected undirected graphs)

| Graph | V | E | Algorithm | Avg time (µs) | MST weight | Edge exams | Comparisons | DSU finds / unions | PQ pushes / pops | Connected |
|---|---|---|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | Kruskal | 58.58 | 134 | 15 | 109 | 30 / 15 | - | true |
| Small | 10 | 30 | Prim | 71.94 | 134 | 60 | 204 | - | 30 / 30 | true |
| Medium | 100 | 500 | Kruskal | 206.68 | 1153 | 180 | 3839 | 360 / 180 | - | true |
| Medium | 100 | 500 | Prim | 191.99 | 1153 | 1000 | 7770 | - | 500 / 500 | true |
| Large | 1000 | 5000 | Kruskal | 685.64 | 12900 | 3271 | 50394 | 6542 / 3271 | - | true |
| Large | 1000 | 5000 | Prim | 1107.62 | 12900 | 10000 | 111028 | - | 5000 / 5000 | true |

Kruskal and Prim returned the same MST weight on all three graphs: **yes**.

## 2. DSU optimisations

### DSU variants used inside Kruskal (same graphs as above)

| Graph | DSU variant | Avg Kruskal time (µs) | find calls | union calls | parent-pointer hops |
|---|---|---|---|---|---|
| Small | none (naive) | 53.68 | 30 | 15 | 22 |
| Small | path compression only | 58.30 | 30 | 15 | 19 |
| Small | union by rank only | 78.64 | 30 | 15 | 20 |
| Small | both | 64.70 | 30 | 15 | 17 |
| Medium | none (naive) | 158.08 | 360 | 180 | 2321 |
| Medium | path compression only | 175.58 | 360 | 180 | 489 |
| Medium | union by rank only | 175.05 | 360 | 180 | 438 |
| Medium | both | 155.27 | 360 | 180 | 290 |
| Large | none (naive) | 1990.68 | 6542 | 3271 | 543294 |
| Large | path compression only | 608.75 | 6542 | 3271 | 12192 |
| Large | union by rank only | 820.60 | 6542 | 3271 | 10625 |
| Large | both | 590.28 | 6542 | 3271 | 6045 |

### DSU worst-case micro-benchmark (n = 5000: build a chain with union(i+1, i), then n x find(0))

| DSU variant | Avg time (µs) | find calls | parent-pointer hops |
|---|---|---|---|
| none (naive) | 57062.02 | 14998 | 24995000 |
| path compression only | 71.17 | 14998 | 9998 |
| union by rank only | 59.95 | 14998 | 9997 |
| both | 47.28 | 14998 | 9997 |

## 3. Topological ordering

### Topological ordering on DAGs and on graphs with cycles

| Graph | V | E | Input type | Is DAG? | Vertices ordered | Order check | Edge exams | Avg time (µs) |
|---|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | random DAG | true | 10 / 10 | valid | 60 | 64.00 |
| Small | 10 | 30 | random directed (has cycles) | false | 1 / 10 | cycle reported | 35 | 28.22 |
| Medium | 100 | 500 | random DAG | true | 100 / 100 | valid | 1000 | 61.77 |
| Medium | 100 | 500 | random directed (has cycles) | false | 2 / 100 | cycle reported | 515 | 43.76 |
| Large | 1000 | 5000 | random DAG | true | 1000 / 1000 | valid | 10000 | 393.94 |
| Large | 1000 | 5000 | random directed (has cycles) | false | 8 / 1000 | cycle reported | 5031 | 84.91 |

## 4. Strongly connected components

### SCC (Tarjan)

| Graph | V | E | Input type | # SCCs | Largest SCC | Edge exams | Avg time (µs) |
|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | random directed (has cycles) | 3 | 8 | 30 | 40.59 |
| Small | 10 | 30 | random DAG | 10 | 1 | 30 | 45.43 |
| Medium | 100 | 500 | random directed (has cycles) | 4 | 97 | 500 | 92.89 |
| Medium | 100 | 500 | random DAG | 100 | 1 | 500 | 80.23 |
| Large | 1000 | 5000 | random directed (has cycles) | 14 | 987 | 5000 | 109.47 |
| Large | 1000 | 5000 | random DAG | 1000 | 1 | 5000 | 124.97 |

## 5. Dijkstra

### Dijkstra from source 0 on the connected undirected graphs

| Graph | V | E | Avg time (µs) | Reachable | Relaxations tried | Successful relaxations | PQ pushes / pops | Comparisons | Max distance |
|---|---|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | 489.55 | 10 / 10 | 60 | 17 | 18 / 18 | 79 | 79 |
| Medium | 100 | 500 | 108.28 | 100 / 100 | 1000 | 202 | 203 / 203 | 2438 | 101 |
| Large | 1000 | 5000 | 474.63 | 1000 / 1000 | 10000 | 1932 | 1933 / 1933 | 35543 | 145 |

## 6. Scaling sweeps (used for the extra plots)

### Sweep A: E = 5V, V grows

| V | E | Kruskal (µs) | Prim (µs) | Dijkstra (µs) | SCC (µs) | Topo DAG (µs) | Topo cyclic (µs) |
|---|---|---|---|---|---|---|---|
| 100 | 500 | 77.97 | 77.19 | 37.65 | 22.27 | 10.38 | 6.63 |
| 200 | 1000 | 142.49 | 145.28 | 90.50 | 33.17 | 17.37 | 5.64 |
| 400 | 2000 | 279.88 | 351.86 | 161.64 | 46.01 | 31.81 | 9.34 |
| 800 | 4000 | 465.54 | 1599.28 | 452.00 | 139.30 | 194.64 | 20.19 |
| 1600 | 8000 | 1140.73 | 2571.68 | 1160.12 | 238.04 | 168.86 | 33.19 |
| 3200 | 16000 | 2427.78 | 3799.69 | 1698.88 | 362.87 | 277.86 | 32.41 |

### Sweep B: V = 1000, E grows

| V | E | Kruskal (µs) | Prim (µs) | Dijkstra (µs) | SCC (µs) | Topo DAG (µs) | Topo cyclic (µs) |
|---|---|---|---|---|---|---|---|
| 1000 | 2000 | 253.78 | 438.49 | 240.71 | 75.96 | 49.15 | 19.43 |
| 1000 | 4000 | 470.95 | 753.93 | 346.04 | 82.29 | 66.21 | 12.24 |
| 1000 | 8000 | 880.32 | 2340.24 | 535.85 | 111.22 | 82.76 | 16.30 |
| 1000 | 16000 | 1682.54 | 3789.90 | 908.58 | 182.87 | 156.28 | 25.93 |
| 1000 | 32000 | 3545.51 | 7543.40 | 1151.18 | 315.93 | 268.89 | 64.05 |
| 1000 | 64000 | 6868.32 | 24918.44 | 1837.70 | 616.48 | 489.79 | 160.96 |

