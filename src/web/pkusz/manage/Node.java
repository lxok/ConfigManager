package web.pkusz.manage;

import jdk.internal.util.xml.impl.ReaderUTF8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by nick on 2017/9/14.
 */
public class Node {

    public static final int IP_VALUE_LENGTH = 15;

    String id;
    String ip;
    String port;
    long lastChangedTime;

    Node(String id, String ip, String port, long lastChangedTime) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.lastChangedTime = lastChangedTime;
    }

    Node(String id, byte[] val) {
        this.id = id;

    }

    byte[] serialize() {
        return ip.getBytes();
    }

    void deserialize(byte[] val) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(val);
            Reader r = new ReaderUTF8(bais);
            char[] c = new char[IP_VALUE_LENGTH];
            r.read(c);
            ip = String.valueOf(c);
        } catch (IOException e) {
            //log
            System.out.println("Node deserialize failed.");
            System.out.println(e.toString());
        }
    }
}
