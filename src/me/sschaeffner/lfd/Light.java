package me.sschaeffner.lfd;

/**
 * Created by s on 18/09/16.
 */
public class Light extends Luminary {

    private long address;

    public Light(Lightify conn, long address, String name) {
        super(name);

        this.conn = conn;
        this.address = address;
    }

    protected void updateStatus(boolean on, int luminance, int temperature, byte r, byte g, byte b) {
        this.on = on;
        this.luminance = luminance;
        this.temperature = temperature;
        this.r = r;
        this.g = g;
        this.b = b;
    }


    public long getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Light{" +
                "address=" + address +
                '}';
    }

    @Override
    protected byte[] buildCommand(byte command, byte[] data) {
        return conn.buildLightCommand(command, this, data);
    }
}
