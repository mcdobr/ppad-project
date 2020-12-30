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
import java.util.stream.Stream;

import static java.util.Objects.isNull;

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


    private final List<SpanningTreeMessage> responses = new ArrayList<>();
    private Integer parentId;

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
//        seenUuids.clear();
        SpanningTreeMessage message = reader.readMessage();

        if (seenUuids.contains(message.getMessageUuid())) {
            seenUuids.add(message.getMessageUuid());

            if (message.getType() == SpanningTreeMessageType.BUILD) {
                if (isNull(parentId)) {
                    parentId = message.getSourceId();
                    Map<Node, SpanningTreeMessage> neighborBuildMessagesToSend = neighbors.keySet()
                            .stream()
                            .filter(node -> node.getId() != parentId)
                            .collect(Collectors.toMap(
                                    Function.identity(),
                                    neighbor -> new SpanningTreeMessage(this, neighbor, UUID.randomUUID(), SpanningTreeMessageType.BUILD))
                            );

                    IntStream.range(0, SpanningTreeProtocol.NUMBER_OF_TRIES)
                            .forEach(i -> neighborBuildMessagesToSend.forEach((destination, messageForDestination) -> writer.writeMessage(messageForDestination)));
                }
            } else {
                responses.add(message);
                if (seenUuids.size() >= neighbors.size()) {
                    List<Integer> reversedTopologicalSort = Stream.concat(
                            responses.stream()
                                    .map(SpanningTreeMessage::getReversedTopologicalSort)
                                    .flatMap(List::stream),
                            Stream.of(this.id)
                    ).collect(Collectors.toList());

                    List<Integer> reversedParent = Stream.concat(
                            responses.stream()
                                    .map(SpanningTreeMessage::getReversedParent)
                                    .flatMap(List::stream),
                            Stream.of(parentId)
                    ).collect(Collectors.toList());

                    SpanningTreeMessage resultForParent = new SpanningTreeMessage(
                            this.id,
                            parentId,
                            message.getDestinationAddress(),
                            message.getSourceAddress(),
                            this.getPort(),
                            message.getSourcePort(),
                            message.getMessageUuid(),
                            SpanningTreeMessageType.RESULT,
                            reversedTopologicalSort,
                            reversedParent
                    );
                    writer.writeMessage(resultForParent);
                }
            }
        }
    }
}
