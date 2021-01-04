package me.mircea;

import me.mircea.shared.Node;

import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        final int startPort = 16_000;
        final int numberOfNodes = 8;

        Map<Integer, Node> nodeMap = IntStream.range(0, numberOfNodes)
                .mapToObj(id -> {
                    try {
                        return new Node(id, startPort + id);
                    } catch (SocketException e) {
                        throw new IllegalStateException("Could not bind to UDP id " + id, e);
                    }
                })
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        addUndirectedEdgeWithLatency(nodeMap, 0, 1, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 0, 2, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 1, 3, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 1, 4, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 2, 4, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 2, 5, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 5, 6, ThreadLocalRandom.current().nextInt());
        addUndirectedEdgeWithLatency(nodeMap, 6, 7, ThreadLocalRandom.current().nextInt());

        /**
         * @implNote Since I am using threads to showcase that this is possible over UDP, I am basically simulating
         * processes competing for CPU time. Since the executor will start a task only when another task was completed,
         * running with less threads than the number of nodes might be ok, but it also might cause starvation because
         * the tasks currently running could be waiting for their children's results, but those children tasks do not
         * get CPU time.
         *
         * @implNote Without doing a formal proof, for any G = (V, E) the number of minimum threads you would need is
         * *PROBABLY* O(diameter of G) since the worst case is that you start from one endpoint of the diameter and have
         * to wait for all the children to finish (including the one that is a diameter apart from it).
         *
         */
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfNodes);
        executorService.invokeAll(nodeMap.values().stream().map(Server::toCallable).collect(Collectors.toList()));
    }

    private static void addUndirectedEdgeWithLatency(Map<Integer, Node> nodeMap, int u, int v, int latency) {
        nodeMap.get(u).addNeighbor(nodeMap.get(v), latency);
        nodeMap.get(v).addNeighbor(nodeMap.get(u), latency);
    }

    private static Callable<Void> toCallable(final Runnable runnable) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        };
    }

}
