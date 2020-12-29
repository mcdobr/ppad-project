package me.mircea;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SpanningTreeMessage implements Serializable {
    private static final int CLIENT_ID = -1;
    private static final InetAddress ADDRESS;
    static {
        try {
            ADDRESS = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not instantiate Node class");
        }
    }

    private final int sourceId;
    private final int destinationId;
    private final InetAddress sourceAddress;
    private final InetAddress destinationAddress;
    private final int sourcePort;
    private final int destinationPort;
    private final UUID messageUuid;
    private final SpanningTreeMessageType type;
    private final List<Integer> reversedTopologicalSort;
    private final List<Integer> reversedParent;

    public SpanningTreeMessage(Node source, Node destination, UUID uuid, SpanningTreeMessageType type) {
        this(source.getId(), destination.getId(), ADDRESS, ADDRESS, source.getPort(), destination.getPort(), uuid, type, new ArrayList<>(), new ArrayList<>());
    }

    public enum SpanningTreeMessageType {
        BUILD,
        RESULT,
        ALREADY // todo: need to check for this to have the number of messages from children not be < number of children
    }
}
