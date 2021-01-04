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
import java.util.stream.Collectors;

public class Client {
    public static void main(String[] args) throws UnknownHostException, SocketException {
        DatagramSocket datagramSocket = new DatagramSocket();
        SpanningTreeMessage spanningTreeMessage = new SpanningTreeMessage(
                SpanningTreeMessage.CLIENT_ID,
                0,
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("127.0.0.1"),
                datagramSocket.getLocalPort(),
                16_000,
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
        List<Integer> parents = new ArrayList<>(response.getReversedTopologicalSort());
        Collections.reverse(parents);
        System.out.println(parents.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","))
        );
    }
}
