package me.mircea.shared;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class SpanningTreeMessageWriter {
    private final DatagramSocket socket;

    public void writeMessage(SpanningTreeMessage message) {
        DatagramPacket packet = serializeMessage(message);
        sendUdpPacket(packet);
    }

    private DatagramPacket serializeMessage(SpanningTreeMessage message) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(message);
            outputStream.flush();

            byte[] responseBuffer = byteArrayOutputStream.toByteArray();
            return new DatagramPacket(responseBuffer,
                    responseBuffer.length,
                    message.getDestinationAddress(),
                    message.getDestinationListeningPort()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not serialize message...", e);
        } finally {
            try {
                if (nonNull(outputStream)) {
                    outputStream.close();
                }
            } catch (IOException ioException) {
                System.err.println("Could not close input stream...");
            }
        }
    }

    private void sendUdpPacket(DatagramPacket responsePacket) {
        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("Could not write");
            e.printStackTrace();
        }
    }
}
