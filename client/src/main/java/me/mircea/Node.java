package me.mircea;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Node implements Runnable {
    private static final int PACKAGE_SIZE = 65_536;

    private final int id;
    private final DatagramSocket socket;
    private final Set<Node> neighbors;

    public Node(int id, int port) throws SocketException {
        this.id = id;
        this.socket = new DatagramSocket(port);
        this.neighbors = new HashSet<>();
    }

    public int getId() {
        return this.id;
    }

    public boolean addNeighbor(Node other) {
        return neighbors.add(other);
    }

    @Override
    public void run() {
        try {
            while (true) {
                try {
                    DatagramPacket requestPacket = readRequest();
                    tryToWriteResponse(requestPacket);
                } catch (IOException e) {
                    System.err.println("Could not read request...");
                    e.printStackTrace();
                }

            }
        } finally {
            socket.close();
        }
    }

    private DatagramPacket readRequest() throws IOException {
        byte[] requestBuffer = new byte[PACKAGE_SIZE];
        DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length);
        socket.receive(requestPacket);
        return requestPacket;
    }

    private void tryToWriteResponse(DatagramPacket requestPacket) {
        try {
            writeResponse(requestPacket);
        } catch (IOException e) {
            System.err.println("Could not write");
            e.printStackTrace();
        }
    }

    private void writeResponse(DatagramPacket requestPacket) throws IOException {
        byte[] responseBuffer = ("Hello world, " + Arrays.toString(requestPacket.getData())).getBytes();
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer,
                responseBuffer.length,
                requestPacket.getAddress(),
                requestPacket.getPort()
        );
        socket.send(responsePacket);
    }
}
