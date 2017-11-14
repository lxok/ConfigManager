package web.pkusz.serialize;

import web.pkusz.data.DatabaseZK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by nick on 2017/10/29.
 */
public class TestNode implements Serialize {

    String ip;
    String port;

    TestNode(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public List<Entry> genEntryList() {
        List<Entry> entries = new ArrayList<>(2);
        entries.add(new Entry("ip", ip));
        entries.add(new Entry("port", port));
        return entries;
    }

    @Override
    public void updateFromEntryList(List<Entry> entries) {}

    public static void main(String[] args) {
        TestNode node = new TestNode("192.168.100.001", "8888");
        DatabaseZK db = new DatabaseZK(new Properties());
        SerializeUtil.setEncrypt(db);
        byte[] res = SerializeUtil.getBytes(node);
        List<Entry> entries = SerializeUtil.getEntries(res);
        Iterator<Entry> i = entries.iterator();
        while (i.hasNext()) {
            Entry entry = i.next();
            System.out.println(entry.key + ":" + entry.value);
        }
    }
}
