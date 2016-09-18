package me.sschaeffner.lfd;

/**
 * Created by s on 18/09/16.
 */
public abstract class Luminary {
    protected Lightify conn;
    protected String name;
    protected boolean on;
    protected int luminance;
    protected int temperature;
    protected byte r, g, b;

    public Luminary(String name) {
        this.name = name;
        this.on = false;
        this.luminance = 0;
        this.temperature = 2000;
        this.r = 0;
        this.g = 0;
        this.b = 0;
    }

    public void setOnOff(boolean on) {
        this.on = on;
        byte[] data = this.conn.buildOnOff(this, on);
        this.conn.send(data);
    }

    public void setLuminance(byte luminance, int time) {
        this.luminance = luminance;
        byte[] data = this.conn.buildLuminance(this, luminance, time);
        this.conn.send(data);
    }

    public void setTemperature(int temperature, int time) {
        this.temperature = temperature;
        byte[] data = this.conn.buildTemp(this, temperature, time);
        this.conn.send(data);
    }

    public void setRGB(byte r, byte g, byte b, int time) {
        this.r = r;
        this.g = g;
        this.b = b;
        byte[] data = this.conn.buildColour(this, r, g, b, time);
        this.conn.send(data);
    }

    public String getName() {
        return name;
    }

    protected abstract byte[] buildCommand(byte command, byte[] data);
}
