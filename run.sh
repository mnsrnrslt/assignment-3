#!/usr/bin/env bash
# Compiles everything, runs the tests, runs the experiments and draws the plots.
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
find src -name '*.java' > sources.txt
javac -d out @sources.txt
rm sources.txt
java -cp out app.Tests
java -cp out app.Benchmark results
python3 scripts/plot.py results
