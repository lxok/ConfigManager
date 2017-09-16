package web.pkusz.node;

import jdk.internal.util.xml.impl.ReaderUTF8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by nick on 2017/9/6.
 */
public class NodeValue {

    public static final int IP_VALUE_LENGTH = 15;

    String ip;

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
            System.out.println("NodeValue deserialize failed.");
            System.out.println(e.toString());
        }
    }
}
