package me.sschaeffner.lfd;

import java.util.HashSet;

/**
 * A group of lights.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public class LfdGroup extends LfdObject {

    /* group's unique identifier */
    private short id;

    /* group's human-readable name */
    private String name;

    /* set of lights belonging to the group */
    private HashSet<Long> lights;

    /**
     * Constructs a new LfdGroup object.
     *
     * @param lfdBridge a reference to the LfdBridge object
     * @param id        the group's unique id
     * @param name      the group's name
     */
    LfdGroup(LfdBridge lfdBridge, short id, String name) {
        super(lfdBridge);
        this.id = id;
        this.name = name;
        this.lights = new HashSet<>();
    }

    @Override
    protected void sendCommand(byte command, byte[] data) {
        byte[] packet = new byte[16 + data.length];

        int length = 14 + data.length;
        byte lengthHi = (byte)((length >> 8) & 0xFF);
        byte lengthLo = (byte)(length & 0xFF);

        byte flag = 0x02;

        byte sequence = lfdBridge.getNextSequence();

        byte[] id = getIdLittleEndian();

        packet[0] = lengthLo;
        packet[1] = lengthHi;
        packet[2] = flag;
        packet[3] = command;
        packet[4] = (byte) 0x00;
        packet[5] = (byte) 0x00;
        packet[6] = (byte) 0x07;
        packet[7] = sequence;
        System.arraycopy(id, 0, packet, 8, 2);
        System.arraycopy(data, 0, packet, 16, data.length);

        lfdBridge.sendPacket(packet);
    }

    /**
     * Sends a request to the bridge to return all information about this group.
     */
    public void requestGroupInfo() throws LfdException {
        synchronized (lfdBridge) {
            if (lfdBridge.getLastRequest() != LfdRequest.NONE) {
                throw new LfdException("cannot send new request while old request is still handled");
            }
            lfdBridge.setLastRequest(LfdRequest.GROUP_INFO);
            sendCommand(LfdOpCodes.GROUP_INFO, new byte[0]);
            lfdBridge.waitForAnswer();
        }
    }

    @Override
    public String toString() {
        return "LfdGroup{" +
                "id=0x" + Long.toHexString(id) +
                ", name='" + name + '\'' +
                ", lights=" + lights +
                '}';
    }

    void setName(String name) {
        this.name = name;
    }

    void setLights(HashSet<Long> lights) {
        this.lights = lights;
    }

    private byte[] getIdLittleEndian() {
        byte idHi = (byte)((id << 8) & 0xFF);
        byte idLo = (byte)(id & 0xFF);
        return new byte[]{idLo, idHi};
    }

    /**
     * Returns the unique id of this group.
     * @return the unique id of this group
     */
    public short getId() {
        return id;
    }

    /**
     * Returns this group's name
     * @return this group's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a set of lights belonging to this group
     * @return a set of lights belonging to this group
     */
    public HashSet<Long> getLights() {
        return lights;
    }
}
