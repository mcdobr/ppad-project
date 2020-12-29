package me.mircea;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.mircea.SpanningTreeMessage.SpanningTreeMessageType;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node implements Runnable {

    @EqualsAndHashCode.Include
    private final int id;
    private final DatagramSocket socket;
    private final Map<Node, Integer> neighbors;
    private final Set<UUID> seenUuids;
    private final SpanningTreeMessageReader reader;
    private final SpanningTreeMessageWriter writer;

    public Node(int id, int port) throws SocketException {
        this.id = id;
        this.socket = new DatagramSocket(port);
        this.neighbors = new HashMap<>();
        this.seenUuids = new HashSet<>();
        this.reader = new SpanningTreeMessageReader(this.socket);
        this.writer = new SpanningTreeMessageWriter(this.socket);
    }

    public int getId() {
        return this.id;
    }

    public int getPort() {
        return socket.getPort();
    }

    public void addNeighbor(Node other, int latency) {
        neighbors.put(other, latency);
    }

    @Override
    public void run() {
        try {
            while (true) {
                buildSpanningTree();
            }
        } finally {
            socket.close();
        }
    }

    private void buildSpanningTree() {
        seenUuids.clear();
        SpanningTreeMessage possibleStartMessage = reader.readMessage();
        if (possibleStartMessage.getType() == SpanningTreeMessageType.BUILD) {
            seenUuids.add(possibleStartMessage.getMessageUuid());
            int parentId = possibleStartMessage.getSourceId();

            Map<Node, SpanningTreeMessage> neighborBuildMessagesToSend = neighbors.keySet()
                    .stream()
                    .filter(node -> node.getId() != parentId)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            neighbor -> new SpanningTreeMessage(this, neighbor, UUID.randomUUID(), SpanningTreeMessageType.BUILD))
                    );

            IntStream.range(0, SpanningTreeProtocol.NUMBER_OF_TRIES)
                    .forEach(i -> neighborBuildMessagesToSend.forEach((destination, messageForDestination) -> writer.writeMessage(messageForDestination)));


            // todo: probably need to rewrite this while such that you only receive messages and look at the type
            List<SpanningTreeMessage> responses = new ArrayList<>();
            while (seenUuids.size() < neighbors.size()) {
                SpanningTreeMessage prospectiveMessage = reader.readMessage();
                if (prospectiveMessage.getType() == SpanningTreeMessageType.RESULT && !seenUuids.contains(prospectiveMessage.getMessageUuid())) {
                    seenUuids.add(prospectiveMessage.getMessageUuid());
                    responses.add(prospectiveMessage);
                }
            }

            List<Integer> reversedTopologicalSort = new ArrayList<>(responses.get(0).getReversedTopologicalSort());
            reversedTopologicalSort.add(this.id);

            List<Integer> reversedParent = new ArrayList<>(responses.get(0).getReversedParent());
            reversedParent.add(parentId);

            SpanningTreeMessage resultForParent = new SpanningTreeMessage(
                    this.id,
                    parentId,
                    possibleStartMessage.getDestinationAddress(),
                    possibleStartMessage.getSourceAddress(),
                    this.getPort(),
                    possibleStartMessage.getSourcePort(),
                    possibleStartMessage.getMessageUuid(),
                    SpanningTreeMessageType.RESULT,
                    reversedTopologicalSort,
                    reversedParent
            );
            writer.writeMessage(resultForParent);
        }
    }
}
