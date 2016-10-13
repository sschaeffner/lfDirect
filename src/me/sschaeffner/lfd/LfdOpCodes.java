package me.sschaeffner.lfd;

/**
 * A list of opcodes used by the bridge.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
class LfdOpCodes {
    static final byte ALL_LIGHTS_STATUS = 0x13; //returns list of light address, light status, light name
    static final byte GROUP_LIST = 0x1E; //returns list of group id and group name
    static final byte GROUP_INFO = 0x26; //returns group id, group name, and list of light addresses
    static final byte LUMINANCE = 0x31; //set group luminance
    static final byte ONOFF = 0x32; //set group onoff
    static final byte TEMPERATURE = 0x33; //set group colour temperature
    static final byte COLOUR = 0x36; //set group colour
}
