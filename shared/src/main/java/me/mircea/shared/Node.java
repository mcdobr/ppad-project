package me.mircea.shared;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import me.mircea.shared.SpanningTreeMessage.SpanningTreeMessageType;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static me.mircea.shared.SpanningTreeMessage.CLIENT_ID;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node implements Runnable {
    @ToString.Include
    @EqualsAndHashCode.Include
    private final int id;

    private final DatagramSocket socket;
    private final Map<Node, Integer> neighbors;
    private final SpanningTreeMessageReader reader;
    private final SpanningTreeMessageWriter writer;
    private final List<SpanningTreeMessage> messagesReceived = new ArrayList<>();

    private UUID session;
    @ToString.Include
    private Integer parentId;

    public Node(int id, int port) throws SocketException {
        this.id = id;
        this.socket = new DatagramSocket(port);
        this.neighbors = new HashMap<>();
        this.reader = new SpanningTreeMessageReader(this.socket);
        this.writer = new SpanningTreeMessageWriter(this.socket, 1);
    }

    public int getId() {
        return this.id;
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public void addNeighbor(Node other, int latency) {
        neighbors.put(other, latency);
    }

    @Override
    public void run() {
        try {
//            while (true) {
            buildSpanningTree();

//                Thread.sleep(500);
//                messagesReceived.clear();
//                parentId = null;
//            }
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
        } finally {
            socket.close();
        }
    }

    private void buildSpanningTree() {
        while (!hasTalkedToAllNeighbors()) {
            SpanningTreeMessage message = reader.readMessage();
            handleMessage(message);
        }
        SpanningTreeMessage messageFromParent = messagesReceived.stream()
                .filter(msg -> msg.getSourceId() == parentId)
                .findAny()
                .orElseThrow();
        reportToParent(messageFromParent);
    }

    private void handleMessage(SpanningTreeMessage message) {
        if (messagesReceived.stream().noneMatch(msg -> msg.getMessageUuid().equals(message.getMessageUuid()))) {
            messagesReceived.add(message);
            if (message.getType() == SpanningTreeMessageType.BUILD) {
                if (isNull(parentId)) {
                    askChildrenToBuildSubTree(message);
                } else {
                    sendAlreadyToNeighbor(message);
                }
            }
        }
    }

    private void askChildrenToBuildSubTree(SpanningTreeMessage messageFromParent) {
        parentId = messageFromParent.getSourceId();
        Map<Node, SpanningTreeMessage> neighborBuildMessagesToSend = neighbors.keySet()
                .stream()
                .filter(node -> node.getId() != parentId)
                .collect(Collectors.toMap(
                        Function.identity(),
                        neighbor -> createBuildMessage(this, neighbor)
                        )
                );

        neighborBuildMessagesToSend.forEach(
                (destination, messageForDestination) -> writer.writeMessage(messageForDestination)
        );
    }

    private void sendAlreadyToNeighbor(SpanningTreeMessage message) {
        SpanningTreeMessage alreadyMessage = new SpanningTreeMessage(
                message.getDestinationId(),
                message.getSourceId(),
                message.getDestinationAddress(),
                message.getSourceAddress(),
                message.getDestinationListeningPort(),
                message.getSourceListeningPort(),
                message.getMessageUuid(),
                SpanningTreeMessageType.ALREADY,
                Collections.emptyList(),
                Collections.emptyList(),
                message.getSimulatedLatency()
        );
        log.debug("{} sending ALREADY message to {}", this.id, message.getSourceId());
        writer.writeMessage(alreadyMessage);
    }

    private void reportToParent(SpanningTreeMessage messageFromParent) {
        List<Integer> reversedTopologicalSort = Stream.concat(
                messagesReceived.stream()
                        .map(SpanningTreeMessage::getReversedTopologicalSort)
                        .flatMap(List::stream),
                Stream.of(this.id)
        ).collect(Collectors.toList());

        List<Integer> reversedParent = Stream.concat(
                messagesReceived.stream()
                        .map(SpanningTreeMessage::getReversedParent)
                        .flatMap(List::stream),
                Stream.of(parentId)
        ).collect(Collectors.toList());

        SpanningTreeMessage resultForParent = new SpanningTreeMessage(
                this.id,
                parentId,
                messageFromParent.getDestinationAddress(),
                messageFromParent.getSourceAddress(),
                this.getPort(),
                messageFromParent.getSourceListeningPort(),
                messageFromParent.getMessageUuid(),
                SpanningTreeMessageType.RESULT,
                reversedTopologicalSort,
                reversedParent,
                messageFromParent.getSimulatedLatency()
        );
        log.debug("{} sending message to {}", this.id, parentId);
        writer.writeMessage(resultForParent);
    }

    private boolean hasTalkedToAllNeighbors() {
        if (isNull(parentId))
            return false;
        long numberOfMessagesReceived = messagesReceived.stream().map(SpanningTreeMessage::getSourceId).distinct().count();
        if (parentId == CLIENT_ID) {
            return numberOfMessagesReceived >= neighbors.size() + 1;
        } else {
            return numberOfMessagesReceived >= neighbors.size();
        }
    }

    private SpanningTreeMessage createBuildMessage(Node source, Node destination) {
        return new SpanningTreeMessage(source,
                destination,
                UUID.randomUUID(),
                SpanningTreeMessageType.BUILD,
                neighbors.get(destination)
        );
    }
}
