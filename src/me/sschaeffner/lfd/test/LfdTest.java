package me.sschaeffner.lfd.test;

import me.sschaeffner.lfd.LfdBridge;
import me.sschaeffner.lfd.LfdException;
import me.sschaeffner.lfd.LfdLight;
import me.sschaeffner.lfd.LoggerImpl;
import org.junit.Test;

import java.io.IOException;

/**
 * Connects to the bridge and executes a couple of commands.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public class LfdTest {
    @Test
    public void testConnection() throws IOException {
        final String BRIDGE_IP = "192.168.0.18";

        LoggerImpl log = new LoggerImpl(LoggerImpl.Loglevel.DEBUG);
        LfdBridge lf = new LfdBridge(BRIDGE_IP, log);

        try {
            lf.requestAllLightsStatus();
            lf.requestGroupList();
        } catch (LfdException e) {
            e.printStackTrace();
        }

        System.out.println("LIGHTS");
        System.out.println(lf.getLights());

        System.out.println("GROUPS");
        System.out.println(lf.getGroups());

        LfdLight li = lf.getLights().get(0);

        li.sendLuminance((byte)0, (short)10);
        //li.sendColour((byte)255, (byte)0, (byte)230, (short)0);
        li.sendTemperature((short)1000, (short)0);

        System.out.println(li);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
