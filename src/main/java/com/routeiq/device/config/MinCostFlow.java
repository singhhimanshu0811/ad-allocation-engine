package com.routeiq.device.config;

import java.util.*;

public class MinCostFlow {

    private final int n;
    private final List<Integer> edgeTo = new ArrayList<>();
    private final List<Long> capacity = new ArrayList<>();
    private final List<Long> cost = new ArrayList<>();
    private final List<List<Integer>> graph;

    public MinCostFlow(int n) {
        this.n = n;
        graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    }

    public int edgeCount() { return edgeTo.size(); }

    public void addEdge(int from, int to, long cap, long costPerUnit) {
        graph.get(from).add(edgeTo.size());
        edgeTo.add(to);
        capacity.add(cap);
        cost.add(costPerUnit);

        graph.get(to).add(edgeTo.size());
        edgeTo.add(from);
        capacity.add(0L);
        cost.add(-costPerUnit);
    }

    public long run(int source, int sink) {
        long totalFlow = 0;
        while (true) {
            long[] dist = new long[n];
            Arrays.fill(dist, Long.MAX_VALUE / 2);
            dist[source] = 0;
            int[] prevEdge = new int[n];
            Arrays.fill(prevEdge, -1);
            boolean[] inQueue = new boolean[n];
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            inQueue[source] = true;

            while (!queue.isEmpty()) {
                int u = queue.poll();
                inQueue[u] = false;
                for (int edgeId : graph.get(u)) {
                    if (capacity.get(edgeId) <= 0) continue;
                    int v = edgeTo.get(edgeId);
                    long nd = dist[u] + cost.get(edgeId);
                    if (nd < dist[v]) {
                        dist[v] = nd;
                        prevEdge[v] = edgeId;
                        if (!inQueue[v]) { queue.add(v); inQueue[v] = true; }
                    }
                }
            }

            if (dist[sink] >= Long.MAX_VALUE / 2) break;

            long pathFlow = Long.MAX_VALUE;
            int v = sink;
            while (v != source) {
                int edgeId = prevEdge[v];
                pathFlow = Math.min(pathFlow, capacity.get(edgeId));
                v = edgeTo.get(edgeId ^ 1);
            }
            v = sink;
            while (v != source) {
                int edgeId = prevEdge[v];
                capacity.set(edgeId, capacity.get(edgeId) - pathFlow);
                capacity.set(edgeId ^ 1, capacity.get(edgeId ^ 1) + pathFlow);
                v = edgeTo.get(edgeId ^ 1);
            }
            totalFlow += pathFlow;
        }
        return totalFlow;
    }

    public long getFlowOnEdge(int forwardEdgeId) {
        return capacity.get(forwardEdgeId ^ 1);
    }
}