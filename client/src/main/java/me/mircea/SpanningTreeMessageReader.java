package me.mircea;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class SpanningTreeMessageReader {
    private static final int PACKAGE_SIZE = 65_536;

    private final DatagramSocket socket;

    public SpanningTreeMessage readMessage() {
        DatagramPacket datagramPacket = receiveUdpPacket();

        return deserializeMessage(datagramPacket);
    }

    private DatagramPacket receiveUdpPacket() {
        byte[] requestBuffer = new byte[PACKAGE_SIZE];
        DatagramPacket datagramPacket = new DatagramPacket(requestBuffer, requestBuffer.length);
        try {
            socket.receive(datagramPacket);
        } catch (IOException ioException) {
            System.err.println("Could not receive message...");
        }
        return datagramPacket;
    }

    private SpanningTreeMessage deserializeMessage(DatagramPacket datagramPacket) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(datagramPacket.getData());
        ObjectInput input = null;
        try {
            input = new ObjectInputStream(byteArrayInputStream);
            Object object = input.readObject();
            if (!(object instanceof SpanningTreeMessage)) {
                throw new IllegalArgumentException("Illegal message...");
            } else {
                return (SpanningTreeMessage) object;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not deserialize message...", e);
        } finally {
            try {
                if (nonNull(input)) {
                    input.close();
                }
            } catch (IOException ioException) {
                System.err.println("Could not close input stream...");
            }
        }
    }
}
