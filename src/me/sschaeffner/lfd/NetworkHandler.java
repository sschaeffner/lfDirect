package me.sschaeffner.lfd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles the "low level" networking.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
final class NetworkHandler {
    /* Default LfdBridge port */
    private static final int PORT = 4000;

    /* PacketReceiver instance that all received packets are passed on to */
    private final PacketReceiver packetReceiver;

    /* Socket connected to the bridge */
    private final Socket socket;

    /* Socket's OutputStream */
    private final OutputStream os;

    /*Socket's InputStream */
    private final InputStream is;


    /**
     * Constructs a new NetworkHandler.
     *
     * @param packetReceiver    an instance of a PacketReceiver
     * @param host              IP address to connect to (the bridge's ip address)
     * @throws IOException      when the connection cannot be established
     */
    NetworkHandler(final PacketReceiver packetReceiver, String host) throws IOException {
        this.packetReceiver = packetReceiver;
        this.socket = new Socket(host, PORT);
        this.os = socket.getOutputStream();
        this.is = socket.getInputStream();

        startListenerThread();
    }

    /**
     * Starts the thread listening to all the network input coming from the bridge.
     */
    private void startListenerThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytesRead;
                try {
                    while ((bytesRead = is.read(buffer)) != -1) {
                        int length = (buffer[1] << 8) + (buffer[0]);
                        if (length == bytesRead - 2) {
                            byte[] input = new byte[bytesRead - 2];
                            System.arraycopy(buffer, 2, input, 0, bytesRead - 2);
                            packetReceiver.onPacketReceive(input);

                            if (packetReceiver.toBeNotifiedOnPacketReceived()) {
                                synchronized (packetReceiver) {
                                    packetReceiver.notifyAll();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                if (!socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Sends a packet to the bridge.
     * @param packet a packet
     */
    void send(byte[] packet) {
        try {
            os.write(packet);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
