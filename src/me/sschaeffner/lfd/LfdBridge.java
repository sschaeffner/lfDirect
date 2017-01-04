package me.sschaeffner.lfd;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The base class to use this API.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public final class LfdBridge implements PacketReceiver {

    /* logger */
    private final LfdLogger logger;

    /* network handler */
    private final NetworkHandler networkHandler;

    /* packet sequence: each packet sent has to have a sequence increased by one */
    private byte sequence;

    /* the type of request that was sent last and is still waiting for an answer */
    private LfdRequest lastRequest;

    /* map of all groups available on the bridge */
    private HashMap<Short, LfdGroup> groups;

    /* map of all lights available to the bridge */
    private HashMap<Long, LfdLight> lights;

    /**
     * Constructs a new LfdBridge object.
     *
     * @param host          the IP address of the bridge
     * @param logger        a LfdLogger instance (can be null to disable logging)
     * @throws IOException  when the connection to the bridge cannot be established
     */
    public LfdBridge(String host, LfdLogger logger) throws IOException {
        this.logger = logger;
        this.networkHandler = new NetworkHandler(this, host);
        this.sequence = 0;
        this.lastRequest = LfdRequest.NONE;
        this.groups = new HashMap<>();
        this.lights = new HashMap<>();
    }

    /**
     * Constructs a new LfdBridge object.
     *
     * @param host          the IP address of the bridge
     * @throws IOException  when the connection to the bridge cannot be established
     */
    public LfdBridge(String host) throws IOException {
        this(host, null);
    }

    /**
     * Requests the bridge to return the current status of all lights.
     */
    public synchronized void requestAllLightsStatus() throws LfdException {
        if (this.lastRequest != LfdRequest.NONE) {
            throw new LfdException("cannot send new request while old request is still handled");
        }
        lastRequest = LfdRequest.ALL_LIGHTS_STATUS;
        sendGlobalCommand(LfdOpCodes.ALL_LIGHTS_STATUS, new byte[]{(byte)0x01});
        waitForAnswer();
    }

    /**
     * Requests the bridge to return a list of all groups.
     */
    public synchronized void requestGroupList() throws LfdException {
        if (this.lastRequest != LfdRequest.NONE) {
            throw new LfdException("cannot send new request while old request is still handled");
        }
        lastRequest = LfdRequest.GROUP_LIST;
        sendGlobalCommand(LfdOpCodes.GROUP_LIST, new byte[0]);
        waitForAnswer();
    }

    void sendPacket(byte[] packet) {
        networkHandler.send(packet);
    }

    void setLastRequest(LfdRequest lastRequest) {
        this.lastRequest = lastRequest;
    }

    byte getNextSequence() {
        return sequence++;
    }

    private void sendGlobalCommand(byte command, byte[] data) {
        byte[] packet = new byte[8 + data.length];

        int length = 6 + data.length;
        byte lengthHi = (byte)((length >> 8) & 0xFF);
        byte lengthLo = (byte)(length & 0xFF);

        byte flag = 0x02;

        byte sequence = getNextSequence();

        packet[0] = lengthLo;
        packet[1] = lengthHi;
        packet[2] = flag;
        packet[3] = command;
        packet[4] = (byte) 0x00;
        packet[5] = (byte) 0x00;
        packet[6] = (byte) 0x07;
        packet[7] = sequence;
        System.arraycopy(data, 0, packet, 8, data.length);

        sendPacket(packet);
    }

    synchronized void waitForAnswer() {
        try {
            this.wait();
            lastRequest = LfdRequest.NONE;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceive(byte[] packet) {
        try {
            switch (lastRequest) {
                case GROUP_LIST:
                    onGroupListPacket(packet);
                    break;
                case GROUP_INFO:
                    onGroupInfoPacket(packet);
                    break;
                case ALL_LIGHTS_STATUS:
                    onAllLightsStatusPacket(packet);
                    break;
                case NONE : default:
                    return;
            }
        } catch (LfdException e) {
            logger.error(e.getMessage() + " " + e.getCause());
        }
    }

    @Override
    public boolean toBeNotifiedOnPacketReceived() {
        return lastRequest != LfdRequest.NONE;
    }

    private void onGroupListPacket(byte[] packet) throws LfdException {
        if (packet.length < 9) {
            throw new LfdException("received packet but too short for a group list packet");
        }

        int groupAmount = (packet[8] << 8) + packet[7];

        if (packet.length < 9 + (18 * groupAmount)) {
            throw new LfdException("corrupt group list packet: too short");
        }

        HashMap<Short, LfdGroup> newGroups = new HashMap<>();

        for (int i = 0; i < groupAmount; i++) {
            int pos = 9 + (18 * i);

            short id = (short) ((packet[pos + 1] << 8) + packet[pos]);

            byte[] nameAscii = new byte[16];
            System.arraycopy(packet, pos + 2, nameAscii, 0, 16);
            String name = new String(nameAscii, Charset.forName("ASCII")).trim();

            if (groups.containsKey(id)) {
                LfdGroup existingGroup = groups.get(id);
                existingGroup.setName(name);
                newGroups.put(id, existingGroup);
            } else {
                newGroups.put(id, new LfdGroup(this, id, name));
            }
            groups = newGroups;
        }
    }

    private void onGroupInfoPacket(byte[] packet) throws LfdException {
        if (packet.length < 26) {
            throw new LfdException("received packet but too short for a group info packet");
        }
        short id = (short) ((packet[8] << 8) + packet[7]);

        byte[] nameAscii = new byte[16];
        System.arraycopy(packet, 9, nameAscii, 0, 16);
        String name = new String(nameAscii, Charset.forName("ASCII")).trim();

        byte amountOfLights = packet[25];

        if (packet.length < 26 + (18*amountOfLights)) {
            throw new LfdException("corrupt group info packet: too short");
        }

        HashSet<Long> lights = new HashSet<>();
        for (int i = 0; i < amountOfLights; i++) {
            int pos = 26 + (18 * i);
            long address =  ((((long) (packet[pos    ] & 0xff)))      ) +
                            ((((long) (packet[pos + 1] & 0xff))) <<  8) +
                            ((((long) (packet[pos + 2] & 0xff))) << 16) +
                            ((((long) (packet[pos + 3] & 0xff))) << 24) +
                            ((((long) (packet[pos + 4] & 0xff))) << 32) +
                            ((((long) (packet[pos + 5] & 0xff))) << 40) +
                            ((((long) (packet[pos + 6] & 0xff))) << 48) +
                            ((((long) (packet[pos + 7] & 0xff))) << 56);
            lights.add(address);
        }

        LfdGroup group;
        if (groups.containsKey(id)) {
            group = groups.get(id);
        } else {
            group = new LfdGroup(this, id, name);
        }
        group.setName(name);
        group.setLights(lights);
    }

    private void onAllLightsStatusPacket(byte[] packet) throws LfdException {
        if (packet.length < 9) {
            throw new LfdException("received packet but too short for all lights status packet");
        }

        int numberOfLights = (packet[8] << 8) + packet[7];

        if (packet.length < 9 + (50*numberOfLights)) {
            throw new LfdException("corrupt all lights status packet: too short");
        }

        for (int i = 0; i < numberOfLights; i++) {
            int pos = 9 + (50 * i);

            long address =  ((((long) (packet[pos + 2] & 0xff)))      ) +
                            ((((long) (packet[pos + 3] & 0xff))) <<  8) +
                            ((((long) (packet[pos + 4] & 0xff))) << 16) +
                            ((((long) (packet[pos + 5] & 0xff))) << 24) +
                            ((((long) (packet[pos + 6] & 0xff))) << 32) +
                            ((((long) (packet[pos + 7] & 0xff))) << 40) +
                            ((((long) (packet[pos + 8] & 0xff))) << 48) +
                            ((((long) (packet[pos + 9] & 0xff))) << 56);

            boolean on = packet[pos + 18] == (byte)0x01;
            byte luminance = packet[pos + 19];
            short temperature = (short)((packet[pos + 21] << 8) + packet[pos + 20]);
            byte r = packet[pos + 22];
            byte g = packet[pos + 23];
            byte b = packet[pos + 24];
            byte h = packet[pos + 25];
            byte[] nameAscii = new byte[16];
            System.arraycopy(packet, pos + 26, nameAscii, 0, 16);
            String name = new String(nameAscii, Charset.forName("ASCII")).trim();

            LfdLight light;
            if (lights.containsKey(address)) {
                light = lights.get(address);
            } else {
                light = new LfdLight(this, address);
                lights.put(address, light);
            }

            light.setOn(on);
            light.setLuminance(luminance);
            light.setTemperature(temperature);
            light.setR(r);
            light.setG(g);
            light.setB(b);
            light.setName(name);
        }
    }

    /**
     * Returns a list of all groups known to the bridge.
     *
     * Make sure to request the list before (requestGroupList()).
     *
     * @return a list of groups known to the bridge
     */
    public List<LfdGroup> getGroups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * Returns a list of all lights known to the bridge.
     *
     * Make sure to request the list before (requestAllLightsStatus()).
     *
     * @return a list of lights known registered with the bridge
     */
    public List<LfdLight> getLights() {
        return new ArrayList<>(lights.values());
    }

    LfdRequest getLastRequest() {
        return lastRequest;
    }

    LfdLogger getLogger() {
        return logger;
    }

    public void shutdown() throws IOException {
        networkHandler.shutdown();
    }

    /* convenience methods */

    /**
     * Outputs an array of bytes in hexadecimal format.
     *
     * @param bytes an array of bytes
     */
    public static void outputBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.println("0x" + String.format("%02x", b) + " ");
            if (i % 8 == 7) System.out.println();
        }
        System.out.println();
    }
}
