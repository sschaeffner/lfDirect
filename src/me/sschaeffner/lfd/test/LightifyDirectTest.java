package me.sschaeffner.lfd.test;

import me.sschaeffner.lfd.Light;
import me.sschaeffner.lfd.Lightify;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by s on 18/09/16.
 */
public class LightifyDirectTest {
    @Test
    public void testConnection() throws IOException {
        Lightify l = new Lightify("192.168.0.18");
        l.updateAllLightStatus();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Light li : l.getLights().values()) {
            System.out.println("*****LIGHT*****");
            System.out.println("name: " + li.getName());
            System.out.println("address: 0x" + Long.toHexString(li.getAddress()));
            li.setLuminance((byte)0x64, 0);
            //li.setRGB((byte)100, (byte)0, (byte)100, 0);
            li.setTemperature(2600, 0);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void outputBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.print("0x" + String.format("%02x", b) + " ");
            if (i % 8 == 7) System.out.println();
        }
        System.out.println();
    }
}
