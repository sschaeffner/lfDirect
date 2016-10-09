package me.sschaeffner.lfd;

/**
 * A list of opcodes used by Lightify.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
class LightifyOpCodes {
    static final byte COMMAND_ALL_LIGHTS_STATUS = 0x13; //returns list of light address, light status, light name
    static final byte COMMAND_GROUP_LIST = 0x1E; //returns list of group id and group name
    static final byte COMMAND_GROUP_INFO = 0x26; //returns group id, group name, and list of light addresses
    static final byte COMMAND_LUMINANCE = 0x31; //set group luminance
    static final byte COMMAND_ONOFF = 0x32; //set group onoff
    static final byte COMMAND_TEMPERATURE = 0x33; //set group color temperature
    static final byte COMMAND_COLOUR = 0x36; //set group color
    static final byte COMMAND_LIGHT_STATUS = 0x68; //returns light address and light status
}
