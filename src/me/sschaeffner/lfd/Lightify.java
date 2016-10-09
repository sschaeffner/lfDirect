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
public final class Lightify implements PacketReceiver {

    /* network handler */
    private final LightifyNetworkHandler networkHandler;

    /* packet sequence: each packet sent has to have a sequence increased by one */
    private byte sequence;

    /* the type of request that was sent last and is still waiting for an answer */
    private LightifyRequest lastRequest;

    /* map of all groups available on the bridge */
    private HashMap<Short, LightifyGroup> groups;

    /* map of all lights available to the bridge */
    private HashMap<Long, LightifyLight> lights;

    /**
     * Constructs a new Lightify object.
     *
     * @param host          the IP address of the Lightify bridge
     * @throws IOException  when the connection to the bridge cannot be established
     */
    public Lightify(String host) throws IOException {
        this.networkHandler = new LightifyNetworkHandler(this, host);
        this.sequence = 0;
        this.lastRequest = LightifyRequest.NONE;
        this.groups = new HashMap<>();
        this.lights = new HashMap<>();
    }

    /**
     * Requests the bridge to return the current status of all lights.
     */
    public synchronized void requestAllLightsStatus() {
        if (this.lastRequest != LightifyRequest.NONE) {
            System.err.println("cannot send new request while old request is still handled");
            return;
        }
        lastRequest = LightifyRequest.ALL_LIGHTS_STATUS;
        sendGlobalCommand(LightifyOpCodes.COMMAND_ALL_LIGHTS_STATUS, new byte[]{(byte)0x01});
        waitForAnswer();
    }

    /**
     * Requests the bridge to return a list of all groups.
     */
    public synchronized void requestGroupList() {
        if (this.lastRequest != LightifyRequest.NONE) {
            System.err.println("cannot send new request while old request is still handled");
            return;
        }
        lastRequest = LightifyRequest.GROUP_LIST;
        sendGlobalCommand(LightifyOpCodes.COMMAND_GROUP_LIST, new byte[0]);
        waitForAnswer();
    }

    void sendPacket(byte[] packet) {
        networkHandler.send(packet);
    }

    void setLastRequest(LightifyRequest lastRequest) {
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
            lastRequest = LightifyRequest.NONE;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceive(byte[] packet) {
        switch (lastRequest) {
            case GROUP_LIST:
                onGroupListPacket(packet);
                break;
            case GROUP_INFO:
                onGroupInfoPacket(packet);
                break;
            case LIGHT_STATUS:
                onLightStatusPacket(packet);
                break;
            case ALL_LIGHTS_STATUS:
                onAllLightsStatusPacket(packet);
                break;
            case NONE : default:
                return;
        }
    }

    @Override
    public boolean toBeNotifiedOnPacketReceived() {
        return lastRequest != LightifyRequest.NONE;
    }

    private void onGroupListPacket(byte[] packet) {
        if (packet.length < 9) {
            System.err.println("received packet but too short for a group list packet");
            return;
        }

        Lightify.outputBytes(packet);

        int groupAmount = (packet[8] << 8) + packet[7];

        if (packet.length < 9 + (18 * groupAmount)) {
            System.err.println("corrupt group list packet: too short");
            return;
        }

        HashMap<Short, LightifyGroup> newGroups = new HashMap<>();

        for (int i = 0; i < groupAmount; i++) {
            int pos = 9 + (18 * i);

            short id = (short) ((packet[pos + 1] << 8) + packet[pos]);

            byte[] nameAscii = new byte[16];
            System.arraycopy(packet, pos + 2, nameAscii, 0, 16);
            String name = new String(nameAscii, Charset.forName("ASCII")).trim();

            if (groups.containsKey(id)) {
                LightifyGroup existingGroup = groups.get(id);
                existingGroup.setName(name);
                newGroups.put(id, existingGroup);
            } else {
                newGroups.put(id, new LightifyGroup(this, id, name));
            }
            groups = newGroups;
        }
    }

    private void onGroupInfoPacket(byte[] packet) {
        if (packet.length < 26) {
            System.err.println("received packet but too short for a group info packet");
            return;
        }
        short id = (short) ((packet[8] << 8) + packet[7]);

        byte[] nameAscii = new byte[16];
        System.arraycopy(packet, 9, nameAscii, 0, 16);
        String name = new String(nameAscii, Charset.forName("ASCII")).trim();

        byte amountOfLights = packet[25];

        if (packet.length < 26 + (18*amountOfLights)) {
            System.err.println("corrupt group info packet: too short");
            return;
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

        LightifyGroup group;
        if (groups.containsKey(id)) {
            group = groups.get(id);
        } else {
            group = new LightifyGroup(this, id, name);
        }
        group.setName(name);
        group.setLights(lights);
    }

    private void onLightStatusPacket(byte[] packet) {
        if (packet.length < 35) {
            System.err.println("received packet but too short for light status packet");
            return;
        }

        boolean on = packet[27] == (byte)0x01;
        byte luminance = packet[28];
        short temperature = (short)((packet[30] << 8) + packet[29]);
        byte r = packet[31];
        byte g = packet[32];
        byte b = packet[33];
        byte h = packet[34];//possibly hue?

        throw new RuntimeException("Not implemented yet");
    }

    private void onAllLightsStatusPacket(byte[] packet) {
        if (packet.length < 9) {
            System.err.println("received packet but too short for all lights status packet");
            return;
        }

        int numberOfLights = (packet[8] << 8) + packet[7];

        if (packet.length < 9 + (50*numberOfLights)) {
            System.err.println("corrupt all lights status packet: too short");
            return;
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

            LightifyLight light;
            if (lights.containsKey(address)) {
                light = lights.get(address);
            } else {
                light = new LightifyLight(this, address);
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
    public List<LightifyGroup> getGroups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * Returns a list of all lights known to the bridge.
     *
     * Make sure to request the list before (requestAllLightsStatus()).
     *
     * @return a list of lights known registered with the bridge
     */
    public List<LightifyLight> getLights() {
        return new ArrayList<>(lights.values());
    }

    LightifyRequest getLastRequest() {
        return lastRequest;
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
            System.out.print("0x" + String.format("%02x", b) + " ");
            if (i % 8 == 7) System.out.println();
        }
        System.out.println();
    }
}
