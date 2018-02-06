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
