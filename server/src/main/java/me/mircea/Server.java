package me.mircea;

import me.mircea.shared.Node;

import java.net.SocketException;
import java.util.concurrent.ThreadLocalRandom;

import static me.mircea.shared.ClusterConfig.LATENCY_UPPER_BOUND_IN_MS;
import static me.mircea.shared.ClusterConfig.NUMBER_OF_NODES;
import static me.mircea.shared.ClusterConfig.START_PORT;

/**
 * @implNote Without doing a formal proof, for any G = (V, E) the number of minimum threads you would need is
 * *PROBABLY* O(diameter of G) since the worst case is that you start from one endpoint of the diameter and have
 * to wait for all the children to finish (including the one that is a diameter apart from it).
 */

public class Server {
    public static void main(String[] args) throws SocketException {
        int id = Integer.parseInt(args[0]);
        Node node = new Node(id, START_PORT + id);

        for (int neighbor = 0; neighbor < NUMBER_OF_NODES; ++neighbor) {
            if (neighbor != id) {
                node.getNeighbors().put(neighbor, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
            }
        }

        node.run();

//        addUndirectedEdgeWithLatency(nodeMap, 0, 1, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 0, 2, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 1, 3, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 1, 4, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 2, 4, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 2, 5, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 5, 6, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
//        addUndirectedEdgeWithLatency(nodeMap, 6, 7, ThreadLocalRandom.current().nextInt(LATENCY_UPPER_BOUND_IN_MS));
    }
}
