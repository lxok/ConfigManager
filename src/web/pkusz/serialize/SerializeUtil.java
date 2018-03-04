package web.pkusz.serialize;

import jdk.internal.util.xml.impl.ReaderUTF8;
import web.pkusz.data.DatabaseZK;
import web.pkusz.encrypt.Encryptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by nick on 2017/10/23.
 */
/**
 SerializeUtil工具类接口可以将可以将属性序列与字节序列之间相互转换。
 字节序列可以直接被写入zk路径下的值。
 每个Serialize对象的属性序列是由一串属性项组成而成的，属性项由Entry类实现，每个Entry类内部包含一个key和一个value,一般来说，当
 某个Serialize对象生成属性项序列时，key就是对象属性的名称，value为该对象属性对应的值。

 在SerializeUtil类中实现时，首先将一个属性项序列转换为一个JSON结构，再将该JSON格式的字符串使用utf-8编码生成字节序列。
 反之，可以将字节序列转换为属性项序列。
 */
public class SerializeUtil {

    private static boolean encrypt = false;
    private static Encryptor encryptor = null;

    public static boolean setEncrypt(DatabaseZK db) {
        Encryptor e = new Encryptor();
        if (!e.init(db)) {
            return false;
        }
        encryptor = e;
        encrypt = true;
        return true;
    }

    public static byte[] getBytes(Serialize tar) {
        List<Entry> entries = tar.genEntryList();
        if (entries.size() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        Iterator<Entry> i = entries.iterator();
        while (i.hasNext()) {
            Entry entry = i.next();
            sb.append("\"");
            sb.append(entry.key);
            sb.append("\"");
            sb.append(":");
            sb.append("\"");
            sb.append(entry.value);
            sb.append("\"");
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        byte[] res = sb.toString().getBytes();
        if (encrypt) {
            return encryptor.encrpyt(res);
        }
        return res;
    }

    public static List<Entry> getEntries(byte[] bytes) {
        List<Entry> entries = new ArrayList<>();
        if (bytes == null || bytes.length == 0) {
            return entries;
        }
        try {
            if (encrypt) {
                bytes = encryptor.decrypt(bytes);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            Reader r = new ReaderUTF8(bais);
            char[] c = new char[bytes.length];
            r.read(c);
            Entry e = null;
            int i = 0;
            boolean keyValueFlag = true;
            while (i < c.length && c[i] != '\0') {
                if (c[i] == '\"' || c[i] == ':' || c[i] == ',' || c[i] == '{' || c[i] == '}') {
                    i++;
                    continue;
                }
                int j = i++;
                while (!(c[i] == '\"' || c[i] == ':' || c[i] == ',' || c[i] == '{' || c[i] == '}')) {
                    i++;
                }
                String str = String.valueOf(c, j, i - j);
                if (keyValueFlag) {
                    e = new Entry();
                    e.key = str;
                    keyValueFlag = false;
                } else {
                    e.value = str;
                    entries.add(e);
                    keyValueFlag = true;
                }
                i++;
            }
        } catch (IOException e) {
            //log
            System.out.println("SerializeUtil get entries from bytes failed.");
            System.out.println(e.toString());
        }
        return entries;
    }

    public static List<Entry> parseCharArray(char[] c) {
        List<Entry> entries = new ArrayList<>();
        Entry e = null;
        int i = 0;
        boolean keyValueFlag = true;
        while (i < c.length && c[i] != '\0') {
            if (c[i] == '\"' || c[i] == ':' || c[i] == ',' || c[i] == '{' || c[i] == '}') {
                i++;
                continue;
            }
            int j = i++;
            while (!(c[i] == '\"' || c[i] == ':' || c[i] == ',' || c[i] == '{' || c[i] == '}')) {
                i++;
            }
            String str = String.valueOf(c, j, i - j);
            if (keyValueFlag) {
                e = new Entry();
                e.key = str;
                keyValueFlag = false;
            } else {
                e.value = str;
                entries.add(e);
                keyValueFlag = true;
            }
            i++;
        }
        return entries;
    }

    public static String getStringFromEntries(List<Entry> entries) {
        if (entries.size() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        Iterator<Entry> i = entries.iterator();
        while (i.hasNext()) {
            Entry entry = i.next();
            sb.append("\"");
            sb.append(entry.key);
            sb.append("\"");
            sb.append(":");
            sb.append("\"");
            sb.append(entry.value);
            sb.append("\"");
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
