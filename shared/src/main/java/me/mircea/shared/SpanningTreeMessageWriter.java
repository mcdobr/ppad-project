package me.mircea.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@Slf4j
public class SpanningTreeMessageWriter {
    private final DatagramSocket socket;
    private final int numberOfTries;

    public void writeMessage(SpanningTreeMessage message) {
        IntStream.range(0, numberOfTries).forEach(index -> {
            log.debug("Sending message from {} to {}: {}",
                    message.getSourceId(),
                    message.getDestinationId(),
                    message.getType()
            );
            DatagramPacket packet = serializeMessage(message);
            simulateLatency(message.getSimulatedLatency());
            sendUdpPacket(packet);
        });
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
                log.error("Could not close input stream...");
            }
        }
    }

    private void sendUdpPacket(DatagramPacket responsePacket) {
        log.debug("Trying to write UDP packet to {}:{}", responsePacket.getAddress(), responsePacket.getPort());
        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            log.warn("Could not write UDP packet to {}:{}", responsePacket.getAddress(), responsePacket.getPort());
        }
    }

    private void simulateLatency(int latency) {
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
