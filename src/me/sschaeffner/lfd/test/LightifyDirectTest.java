package me.sschaeffner.lfd.test;

import me.sschaeffner.lfd.Lightify;
import me.sschaeffner.lfd.LightifyLight;
import org.junit.Test;

import java.io.IOException;

/**
 * Connects to the bridge and executes a couple of simple commands.
 *
 * @author Simon Schäffner (simon.schaeffner@googlemail.com)
 */
public class LightifyDirectTest {
    @Test
    public void testConnection() throws IOException {
        final String BRIDGE_IP = "192.168.0.18";

        Lightify lf = new Lightify(BRIDGE_IP);
        lf.requestAllLightsStatus();
        lf.requestGroupList();

        System.out.println("LIGHTS");
        System.out.println(lf.getLights());

        System.out.println("GROUPS");
        System.out.println(lf.getGroups());

        LightifyLight li = lf.getLights().get(0);

        li.sendLuminance((byte)100, (short)10);
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
