package me.mircea.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.mircea.shared.ClusterConfig.START_PORT;

@Getter
@AllArgsConstructor
public class SpanningTreeMessage implements Serializable {
    public static final int CLIENT_ID = -1;
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
    private final int sourceListeningPort;
    private final int destinationListeningPort;
    private final UUID messageUuid;
    private final SpanningTreeMessageType type;
    private final List<Integer> reversedTopologicalSort;
    private final List<Integer> reversedParent;
    private final int simulatedLatency;

    public SpanningTreeMessage(Node source, int destinationId, UUID uuid, SpanningTreeMessageType type, int simulatedLatency) {
        this(source.getId(),
                destinationId,
                ADDRESS,
                ADDRESS,
                source.getPort(),
                destinationId + START_PORT,
                uuid,
                type,
                new ArrayList<>(),
                new ArrayList<>(),
                simulatedLatency
        );
    }

    public enum SpanningTreeMessageType {
        BUILD,
        RESULT,
        ALREADY
    }
}
