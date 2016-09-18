package me.sschaeffner.lfd;

import java.util.ArrayList;

/**
 * Created by s on 18/09/16.
 */
public class Group extends Luminary {

    private int idx;
    private ArrayList<Light> lights;

    public Group(Lightify conn, String name, int idx) {
        super(name);
        this.conn = conn;
        this.idx = idx;
        this.lights = new ArrayList<>();
    }

    protected void setLights(ArrayList<Light> lights) {
        this.lights = lights;
    }

    public int getIdx() {
        return idx;
    }

    public ArrayList<Light> getLights() {
        return lights;
    }

    @Override
    public String toString() {
        return "Group{" +
                "idx=" + idx +
                ", lights=" + lights +
                '}';
    }

    protected byte[] buildCommand(byte command, byte[] data) {
        return conn.buildCommand(command, this, data);
    }
}
