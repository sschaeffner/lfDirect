package me.sschaeffner.lfd;

/**
 * A lfd object. Either a single light or a group of lights.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public abstract class LfdObject {

    /* reference to the LfdBridge object */
    protected final LfdBridge lfdBridge;

    /**
     * Abstract class.
     * @param lfdBridge  reference to the LfdBridge object
     */
    LfdObject(LfdBridge lfdBridge) {
        this.lfdBridge = lfdBridge;
    }

    /**
     * Sends a command to the bridge.
     *
     * @param command   the command's opcode
     * @param data      additional data
     */
    protected abstract void sendCommand(byte command, byte[] data);

    /**
     * Sends an On/Off command to the bridge.
     * @param on    whether the light should be on
     */
    public void sendOnOff(boolean on) {
        byte onOff = on ? (byte)0x01 : (byte)0x00;
        sendCommand(LfdOpCodes.ONOFF, new byte[]{onOff});
    }

    /**
     * Sends a colour temperature command to the bridge (2000-6500 kelvin).
     * @param temperature   the colour temperature in kelvin
     * @param time          the fade time in 1/10s
     */
    public void sendTemperature(short temperature, short time) {
        byte temperatureHi = (byte)((temperature >> 8) & 0xFF);
        byte temperatureLo = (byte)(temperature & 0xFF);
        byte timeHi = (byte)((time >> 8) & 0xFF);
        byte timeLo = (byte)(time & 0xFF);
        sendCommand(LfdOpCodes.TEMPERATURE, new byte[]{temperatureLo, temperatureHi, timeLo, timeHi});
    }

    /**
     * Sends a luminance command to the bridge (0-100).
     * @param luminance the luminance in percent
     * @param time      the fade time in 1/10s
     */
    public void sendLuminance(byte luminance, short time) {
        byte timeHi = (byte)((time >> 8) & 0xFF);
        byte timeLo = (byte)(time & 0xFF);
        sendCommand(LfdOpCodes.LUMINANCE, new byte[]{luminance, timeLo, timeHi});
    }

    /**
     * Sends a colour command to the bridge.
     * @param r     red (0-255)
     * @param g     green (0-255)
     * @param b     blue (0-255)
     * @param time  the fade time in 1/10s
     */
    public void sendColour(byte r, byte g, byte b, short time) {
        byte timeHi = (byte)((time >> 8) & 0xFF);
        byte timeLo = (byte)(time & 0xFF);
        sendCommand(LfdOpCodes.COLOUR, new byte[]{r, g, b, (byte)0xFF, timeLo, timeHi});
    }
}
