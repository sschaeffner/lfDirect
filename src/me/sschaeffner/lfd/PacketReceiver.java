package me.sschaeffner.lfd;

/**
 * An interface to which received packets can be passed on to.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
interface PacketReceiver {
    void onPacketReceive(byte[] packet);
    boolean toBeNotifiedOnPacketReceived();
}
