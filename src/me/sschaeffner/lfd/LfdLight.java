package me.sschaeffner.lfd;

/**
 * A single light.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public class LfdLight extends LfdObject {

    /* light's unique address */
    private long address;

    /* light's human-readable name */
    private String name;

    /* whether the light is on at the moment */
    private boolean on;

    /* the current luminance of the light (off=0x00, full=0x64)*/
    private byte luminance;

    /* the current temperature of the light in Kelvin */
    private short temperature;

    /* the current red value of the light (off=0x00, full=0x64)*/
    private byte r;

    /* the current green value of the light (off=0x00, full=0x64)*/
    private byte g;

    /* the current blue value of the light (off=0x00, full=0x64)*/
    private byte b;

    /**
     * Constructs a new LfdLight object.
     *
     * This method should only be called by the LfdBridge class.
     *
     * @param lfdBridge a reference to the LfdBridge object
     * @param address   the light's unique address
     */
    LfdLight(LfdBridge lfdBridge, long address) {
        super(lfdBridge);
        this.address = address;
    }

    @Override
    protected void sendCommand(byte command, byte[] data) {
        byte[] packet = new byte[16 + data.length];

        int length = 14 + data.length;
        byte lengthHi = (byte)((length >> 8) & 0xFF);
        byte lengthLo = (byte)(length & 0xFF);

        byte flag = 0x00;

        byte sequence = lfdBridge.getNextSequence();

        byte[] address = getAddressLittleEndian();

        packet[0] = lengthLo;
        packet[1] = lengthHi;
        packet[2] = flag;
        packet[3] = command;
        packet[4] = (byte) 0x00;
        packet[5] = (byte) 0x00;
        packet[6] = (byte) 0x07;
        packet[7] = sequence;
        System.arraycopy(address, 0, packet, 8, 8);
        System.arraycopy(data, 0, packet, 16, data.length);

        lfdBridge.sendPacket(packet);
    }

    void setName(String name) {
        this.name = name;
    }

    void setOn(boolean on) {
        this.on = on;
    }

    void setLuminance(byte luminance) {
        this.luminance = luminance;
    }

    void setTemperature(short temperature) {
        this.temperature = temperature;
    }

    void setR(byte r) {
        this.r = r;
    }

    void setG(byte g) {
        this.g = g;
    }

    void setB(byte b) {
        this.b = b;
    }

    /**
     * Returns this light's unique address as little endian.
     * @return this light's address as little endian
     */
    private byte[] getAddressLittleEndian() {
        byte lAdd1 = (byte) ((address >> 56) & 0xff);//MSB
        byte lAdd2 = (byte) ((address >> 48) & 0xff);
        byte lAdd3 = (byte) ((address >> 40) & 0xff);
        byte lAdd4 = (byte) ((address >> 32) & 0xff);
        byte lAdd5 = (byte) ((address >> 24) & 0xff);
        byte lAdd6 = (byte) ((address >> 16) & 0xff);
        byte lAdd7 = (byte) ((address >>  8) & 0xff);
        byte lAdd8 = (byte) ((address      ) & 0xff);//LSB

        return new byte[]{lAdd8, lAdd7, lAdd6, lAdd5, lAdd4, lAdd3, lAdd2, lAdd1};
    }

    @Override
    public String toString() {
        return "LfdLight{" +
                "address=0x" + Long.toHexString(address) +
                ", name='" + name + '\'' +
                ", on=" + on +
                ", luminance=" + (luminance & 0xFF) +
                ", temperature=" + temperature +
                ", r=" + (r & 0xFF) +
                ", g=" + (g & 0xFF) +
                ", b=" + (b & 0xFF) +
                '}';
    }

    /**
     * Returns this light's unique address
     * @return this light's unique address
     */
    public long getAddress() {
        return address;
    }

    /**
     * Returns this light's name.
     * @return  this light's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true when the light is on, false when it is off.
     * @return whether the light is on at the moment
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Returns the light's current luminance (0-100).
     * @return this light's current luminance
     */
    public byte getLuminance() {
        return luminance;
    }

    /**
     * Returns this light's current colour temperature in kelvin (2000-6500).
     * @return this light's current colour temperature
     */
    public short getTemperature() {
        return temperature;
    }

    /**
     * Returns the red part of the light's current colour.
     * @return the red part of the light's current colour
     */
    public byte getR() {
        return r;
    }

    /**
     * Returns the green part of the light's current colour.
     * @return the green part of the light's current colour
     */
    public byte getG() {
        return g;
    }

    /**
     * Returns the blue part of the light's current colour.
     * @return the blue part of the light's current colour
     */
    public byte getB() {
        return b;
    }
}
