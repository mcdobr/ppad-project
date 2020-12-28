package me.mircea;

import java.net.SocketException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Runner {

    public static void main(String[] args) {
        final int startPort = 16_000;
        final int numberOfNodes = 16;

        Map<Integer, Node> nodeMap = IntStream.range(0, numberOfNodes)
                .mapToObj(id -> {
                    try {
                        return new Node(id, startPort + id);
                    } catch (SocketException e) {
                        throw new IllegalStateException("Could not bind to UDP id " + id, e);
                    }
                })
                .collect(Collectors.toMap(Node::getId, Function.identity()));


    }
}
