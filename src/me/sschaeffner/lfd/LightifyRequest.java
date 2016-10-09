package me.sschaeffner.lfd;

/**
 * Which request was sent last to the bridge.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public enum LightifyRequest {
    NONE,
    GROUP_LIST,
    GROUP_INFO,
    LIGHT_STATUS,
    ALL_LIGHTS_STATUS
}
