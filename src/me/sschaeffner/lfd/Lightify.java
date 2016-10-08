package me.sschaeffner.lfd;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by s on 18/09/16.
 */
public class Lightify {

    public static final int PORT = 4000;

    private static final byte COMMAND_ALL_LIGHT_STATUS = 0x13; //returns list of light address, light status, light name
    private static final byte COMMAND_GROUP_LIST = 0x1E; //returns list of group id and group name
    private static final byte COMMAND_GROUP_INFO = 0x26; //returns group id, group name, and list of light addresses
    private static final byte COMMAND_LUMINANCE = 0x31; //set group luminance
    private static final byte COMMAND_ONOFF = 0x32; //set group onoff
    private static final byte COMMAND_TEMP = 0x33; //set group color temperature
    private static final byte COMMAND_COLOUR = 0x36; //set group color
    private static final byte COMMAND_LIGHT_STATUS = 0x68; //returns light address and light status


    private byte seq;
    private HashMap<Long, Light> lights;
    private HashMap<Integer, Group> groups;
    private Socket sock;
    private OutputStream sockOut;
    private InputStream sockIn;

    private LastRequest lastRequest;


    public Lightify(String host) throws IOException {
        this.seq = 1;
        this.lights = new HashMap<>();
        this.groups = new HashMap<>();

        this.sock = new Socket(host, PORT);
        this.sockOut = this.sock.getOutputStream();
        this.sockIn = this.sock.getInputStream();

        this.lastRequest = LastRequest.NONE;

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytesRead;
                try {
                    while ((bytesRead = sockIn.read(buffer)) != -1) {
                        int length = (buffer[1] << 8) + (buffer[0]);
                        if (length == bytesRead - 2) {
                            byte[] input = new byte[bytesRead - 2];
                            System.arraycopy(buffer, 2, input, 0, bytesRead - 2);

                            System.out.println("***** NETWORK INPUT *****");
                            for (int i = 0; i < input.length; i++) {
                                byte b = input[i];
                                System.out.print("0x" + String.format("%02x", b) + " ");

                                if (b >= 0x20) {
                                    System.out.print((char) b);
                                } else {
                                    System.out.print(".");
                                }
                                System.out.print(" ");


                                if (i % 8 == 7) System.out.println();
                            }
                            System.out.println();

                            switch (lastRequest) {
                                case UPDATE_ALL_LIGHT_STATUS:
                                    onAllLightStatusReceive(input);
                                    break;
                                case UPDATE_LIGHT_STATUS:
                                    onLightStatusReceive(input);
                                    break;
                                case GROUP_INFO:
                                    onGroupInfoReceive(input);
                                    break;
                                case GROUP_LIST:
                                    onGroupListDataReceive(input);
                                    break;
                                case NONE:
                                default:

                                    System.out.println("NONE requested, still got network input. ignoring it");
                            }
                        } else {
                            throw new RuntimeException("bytes read and sent length do not match");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Map<Integer, Group> getGroups() {
        return this.groups;
    }

    public Map<Long, Light> getLights() {
        return this.lights;
    }

    public Light getLightByName(String name) {
        Iterator it = lights.entrySet().iterator();
        while (it.hasNext()) {
            Light l = (Light) it.next();
            if (l.getName().equals(name)) return l;
        }
        return null;
    }

    protected byte nextSeq() {
        return ++this.seq;
    }

    protected byte[] buildGlobalCommand(byte command, byte[] data) {
        byte[] bytes = new byte[8 + data.length];

        int length = 6 + data.length;
        byte lengthHi = (byte) ((length >> 8) & 0xff);
        byte lengthLo = (byte) (length & 0xff);

        bytes[0] = lengthLo;
        bytes[1] = lengthHi;
        bytes[2] = 0x02;
        bytes[3] = command;
        bytes[4] = 0x00;
        bytes[5] = 0x00;
        bytes[6] = 0x07;
        bytes[7] = nextSeq();

        System.arraycopy(data, 0, bytes, 8, data.length);

        return bytes;
    }

    protected byte[] buildBasicCommand(byte flag, byte command, byte[] groupOrLight, byte[] data) {
        byte[] bytes = new byte[16 + data.length];

        int length = 14 + data.length;
        byte lengthHi = (byte) ((length >> 8) & 0xff);
        byte lengthLo = (byte) (length & 0xff);

        bytes[0] = lengthLo;
        bytes[1] = lengthHi;
        bytes[2] = flag;
        bytes[3] = command;
        bytes[4] = 0x00;
        bytes[5] = 0x00;
        bytes[6] = 0x07;
        bytes[7] = nextSeq();

        if (groupOrLight.length != 8) throw new IllegalArgumentException("groupOrLight has to be 8 bytes long");
        System.arraycopy(groupOrLight, 0, bytes, 8, 8);

        System.arraycopy(data, 0, bytes, 16, data.length);

        return bytes;
    }


    protected byte[] buildCommand(byte command, Group group, byte[] data) {
        byte idxHi = (byte) ((group.getIdx() >> 8) & 0xff);
        byte idxLo = (byte) ((group.getIdx()) & 0xff);

        return buildBasicCommand((byte) 0x02, command, new byte[]{idxLo, idxHi, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, data);
    }

    protected byte[] buildLightCommand(byte command, Light light, byte[] data) {
        long address = light.getAddress();
        byte lAdd1 = (byte) ((address >> 56) & 0xff);//MSB
        byte lAdd2 = (byte) ((address >> 48) & 0xff);
        byte lAdd3 = (byte) ((address >> 40) & 0xff);
        byte lAdd4 = (byte) ((address >> 32) & 0xff);
        byte lAdd5 = (byte) ((address >> 24) & 0xff);
        byte lAdd6 = (byte) ((address >> 16) & 0xff);
        byte lAdd7 = (byte) ((address >>  8) & 0xff);
        byte lAdd8 = (byte) ((address      ) & 0xff);//LSB

        byte[] addressLittleEndian = new byte[]{lAdd8, lAdd7, lAdd6, lAdd5, lAdd4, lAdd3, lAdd2, lAdd1};

        System.out.println("*****ADDRESS LITTLE ENDIAN*****");
        outputBytes(addressLittleEndian);

        return buildBasicCommand((byte) 0x00, command, new byte[]{lAdd8, lAdd7, lAdd6, lAdd5, lAdd4, lAdd3, lAdd2, lAdd1}, data);
    }

    protected byte[] buildOnOff(Luminary item, boolean on) {
        return item.buildCommand(COMMAND_ONOFF, new byte[]{(byte) (on ? 0x01 : 0x00)});
    }

    protected byte[] buildTemp(Luminary item, int temp, int time) {
        byte tempHi = (byte) ((temp >> 8) & 0xff);
        byte tempLo = (byte) ((temp) & 0xff);

        byte timeHi = (byte) ((time >> 8) & 0xff);
        byte timeLo = (byte) ((time) & 0xff);

        return item.buildCommand(COMMAND_TEMP, new byte[]{tempLo, tempHi, timeLo, timeHi});
    }

    protected byte[] buildLuminance(Luminary item, byte luminance, int time) {
        byte timeHi = (byte) ((time >> 8) & 0xff);
        byte timeLo = (byte) ((time) & 0xff);

        return item.buildCommand(COMMAND_LUMINANCE, new byte[]{luminance, timeLo, timeHi});
    }

    protected byte[] buildColour(Luminary item, byte r, byte g, byte b, int time) {
        byte timeHi = (byte) ((time >> 8) & 0xff);
        byte timeLo = (byte) ((time) & 0xff);

        return item.buildCommand(COMMAND_COLOUR, new byte[]{r, g, b, (byte) 0xff, timeLo, timeHi});
    }

    protected byte[] buildGroupInfo(Group group) {
        return buildCommand(COMMAND_GROUP_INFO, group, new byte[]{});
    }

    protected byte[] buildAllLightStatus(byte flag) {
        return buildGlobalCommand(COMMAND_ALL_LIGHT_STATUS, new byte[]{flag});
    }

    protected byte[] buildLightStatus(Light light) {
        return light.buildCommand(COMMAND_LIGHT_STATUS, new byte[]{});
    }

    protected byte[] buildGroupList() {
        return buildGlobalCommand(COMMAND_GROUP_LIST, new byte[]{});
    }

    public void groupList() {
        if (this.lastRequest == LastRequest.NONE) {
            byte[] data = buildGroupList();
            this.lastRequest = LastRequest.GROUP_LIST;
            send(data);
        } else {
            throw new RuntimeException("lastRequest has to be NONE to send a new request");
        }
    }

    private void onGroupListDataReceive(byte[] data) {
        this.lastRequest = LastRequest.NONE;

        int num = (data[8] << 8) + data[7];

        for (int i = 0; i < num; i++) {
            int pos = 9 + (18 * i);
            byte[] idxB = new byte[2];
            byte[] nameB = new byte[16];
            System.arraycopy(data, pos, idxB, 0, 2);
            System.arraycopy(data, pos + 2, nameB, 0, 16);

            int idx = (idxB[1] << 8) + idxB[0];
            String name = new String(nameB, Charset.forName("ASCII"));

            Group group = new Group(this, name, (byte) idx);
            groups.put(idx, group);
        }
    }

    public void groupInfo(Group group) {
        if (this.lastRequest == LastRequest.NONE) {
            byte[] data = buildGroupInfo(group);
            this.lastRequest = LastRequest.GROUP_INFO;
            send(data);
        } else {
            throw new RuntimeException("lastRequest has to be NONE to send a new request");
        }
    }

    private void onGroupInfoReceive(byte[] data) {
        this.lastRequest = LastRequest.NONE;

        ArrayList<Long> lights = new ArrayList<>();

        byte[] payload = new byte[data.length - 7];
        System.arraycopy(data, 7, payload, 0, payload.length);

        int idx = (payload[1] << 8) + payload[0];

        byte[] nameB = new byte[16];
        System.arraycopy(payload, 2, nameB, 0, 16);
        String name = new String(nameB, Charset.forName("ASCII"));

        int num = payload[18];

        for (int i = 0; i < num; i++) {
            int pos = 7 + 19 + (i * 18);
            payload = new byte[8];
            System.arraycopy(data, pos, payload, 0, 8);

            long address =  ((((long) (payload[0] & 0xff)))      ) +
                            ((((long) (payload[1] & 0xff))) <<  8) +
                            ((((long) (payload[2] & 0xff))) << 16) +
                            ((((long) (payload[3] & 0xff))) << 24) +
                            ((((long) (payload[4] & 0xff))) << 32) +
                            ((((long) (payload[5] & 0xff))) << 40) +
                            ((((long) (payload[6] & 0xff))) << 48) +
                            ((((long) (payload[7] & 0xff))) << 56);

            lights.add(address);
        }
    }

    public void updateLightStatus(Light light) {
        byte[] data = buildLightStatus(light);
        if (this.lastRequest == LastRequest.NONE) {
            this.lastRequest = LastRequest.UPDATE_LIGHT_STATUS;
            send(data);
        } else {
            throw new RuntimeException("lastRequest has to be NONE to send a new request");
        }
    }

    public void onLightStatusReceive(byte[] data) {
        this.lastRequest = LastRequest.NONE;

        //0-26 padding
        boolean on = data[27] != 0;
        byte luminance = data[28];
        int temperature = (data[30] << 8) + data[29];
        byte r = data[31];
        byte g = data[32];
        byte b = data[33];
        byte h = data[34];
    }

    public void updateAllLightStatus() {
        byte[] data = buildAllLightStatus((byte) 0x01);
        if (this.lastRequest == LastRequest.NONE) {
            this.lastRequest = LastRequest.UPDATE_ALL_LIGHT_STATUS;
            send(data);
        } else {
            throw new RuntimeException("lastRequest has to be NONE to send a new request");
        }
    }

    private void onAllLightStatusReceive(byte[] data) {
        this.lastRequest = LastRequest.NONE;

        int num = (data[8] << 8) + data[7];

        System.out.println("num = " + num);

        HashMap<Long, Light> oldLights = lights;
        HashMap<Long, Light> newLights = new HashMap<>();

        for (int i = 0; i < num; i++) {
            int pos = 9 + (i * 50);

            byte[] payload = new byte[42];
            System.arraycopy(data, pos, payload, 0, 42);

            System.out.println("*****PAYLOAD*****");
            outputBytes(payload);


            int a = (payload[1] << 8) + payload[0];

            long address =
                    ((((long) (payload[2] & 0xff)))      ) +
                    ((((long) (payload[3] & 0xff))) <<  8) +
                    ((((long) (payload[4] & 0xff))) << 16) +
                    ((((long) (payload[5] & 0xff))) << 24) +
                    ((((long) (payload[6] & 0xff))) << 32) +
                    ((((long) (payload[7] & 0xff))) << 40) +
                    ((((long) (payload[8] & 0xff))) << 48) +
                    ((((long) (payload[9] & 0xff))) << 56);

            System.out.println("PAYLOAD ADDR: " + Long.toUnsignedString(address));
            System.out.println("PAYLOAD ADDR: " + Long.toHexString(address));

            byte[] status = new byte[16];
            System.arraycopy(payload, 10, status, 0, 16);

            byte[] nameB = new byte[16];
            System.arraycopy(payload, 26, nameB, 0, 16);
            String name = new String(nameB, Charset.forName("ASCII")).trim();

            Light light;
            if (oldLights.containsKey(address)) {
                light = oldLights.get(address);
            } else {
                light = new Light(this, address, name);
            }

            byte[] bsB = new byte[8];
            System.arraycopy(status, 0, bsB, 0, 8);
            String bs = new String(bsB, Charset.forName("ASCII")).trim();

            boolean on = status[8] != 0;
            byte luminance = status[9];
            int temperature = (status[11] << 8) + status[10];
            byte r = status[12];
            byte g = status[13];
            byte b = status[14];
            byte h = status[15];

            System.out.println("STATUS: name=" + name + " on=" + on + " lum=" + luminance + " temp=" + temperature + " r=" + (r & 0xff) + " g=" + (g & 0xff) + " b=" + (b & 0xff) + " bs=" + bs + " h=" + (h & 0xff));


            newLights.put(address, light);
        }

        lights = newLights;
    }

    protected void send(byte[] data) {
        try {
            this.sockOut.write(data);
            this.sockOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void outputBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.print("0x" + String.format("%02x", b) + " ");

            if (b >= 0x20) {
                System.out.print((char) b);
            } else {
                System.out.print(".");
            }
            System.out.print(" ");

            if (i % 8 == 7) System.out.println();
        }
        System.out.println();
    }


    private enum LastRequest {
        NONE, UPDATE_ALL_LIGHT_STATUS, UPDATE_LIGHT_STATUS, GROUP_LIST, GROUP_INFO
    }
}
