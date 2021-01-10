package me.mircea;

import me.mircea.shared.SpanningTreeMessage;
import me.mircea.shared.SpanningTreeMessage.SpanningTreeMessageType;
import me.mircea.shared.SpanningTreeMessageReader;
import me.mircea.shared.SpanningTreeMessageWriter;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.mircea.shared.ClusterConfig.NUMBER_OF_NODES;
import static me.mircea.shared.ClusterConfig.START_PORT;

public class Client {
    public static void main(String[] args) throws SocketException {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            int numberOfRuns = 1;
            IntStream.range(0, numberOfRuns)
                    .forEach(runNumber -> {
                        int destinationId = ThreadLocalRandom.current().nextInt(NUMBER_OF_NODES);
                        System.out.println("Starting run by setting root to be " + destinationId + "...");
                        try {
                            askForSpanningTree(datagramSocket, destinationId);
                        } catch (UnknownHostException e) {
                            System.err.println("Could not resolve host...");
                        }
                    });
        }
    }

    private static void askForSpanningTree(DatagramSocket datagramSocket, int destinationId) throws UnknownHostException {
        SpanningTreeMessage spanningTreeMessage = new SpanningTreeMessage(
                SpanningTreeMessage.CLIENT_ID,
                destinationId,
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("127.0.0.1"),
                datagramSocket.getLocalPort(),
                START_PORT + destinationId,
                UUID.randomUUID(),
                SpanningTreeMessageType.BUILD,
                Collections.emptyList(),
                Collections.emptyList()
        );

        SpanningTreeMessageWriter writer = new SpanningTreeMessageWriter(datagramSocket, 1);
        writer.writeMessage(spanningTreeMessage);

        SpanningTreeMessageReader reader = new SpanningTreeMessageReader(datagramSocket);
        SpanningTreeMessage response = reader.readMessage();

        System.out.println("Topological sort: ");
        List<Integer> topologicalSort = new ArrayList<>(response.getReversedTopologicalSort());
        Collections.reverse(topologicalSort);
        System.out.println(topologicalSort.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","))
        );

        System.out.println("Parents: ");
        List<Integer> parents = new ArrayList<>(response.getReversedParent());
        Collections.reverse(parents);
        System.out.println(parents.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","))
        );
    }
}
