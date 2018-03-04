package web.pkusz.protocal;

import web.pkusz.serialize.Entry;
import web.pkusz.serialize.Serialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 2018/1/6.
 */
/**
 NodePersConfig类是zk路径/mimic/node/persistent/路径下CM节点子路径的关联值。
 NodePersConfig类实现了Serialize接口，表示它可以被序列化和反序列化。
 当CM服务器发现新的CM节点被注册后，会创建与节点id值对应的路径，该路径下的关联值被在系统执行时被抽象为该类。
 Serialize接口使其对象状态转换为一个键值序列，再利用SerializeUtil工具可以实现字节序列到键值序列之间的转换。
 */
public class NodePersConfig implements Serialize {

    long opNum;
    int state;
    Entry[] attributes;

    public NodePersConfig(long opNum, int state, Entry[] attributes) {
        this.opNum = opNum;
        this.state = state;
        this.attributes = attributes;
    }

    public NodePersConfig() {
        opNum = 0;
        state = 0;
        attributes = null;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(opNum);
        s.append(" ");
        s.append(state);
        if (attributes != null  && attributes.length > 0) {
            s.append(" ");
            for (Entry e : attributes) {
                s.append(e.toString());
                s.append(";;");
            }
        }
        return s.toString();
    }

    public long getOpNum() {
        return opNum;
    }

    public int getState() {
        return state;
    }

    public String getAttribute(String key) {
        if (attributes == null || attributes.length == 0) {
            return null;
        }
        for (int i = 0; i < attributes.length; i++) {
            if (key.equals(attributes[i].getKey())) {
                return attributes[i].value;
            }
        }
        return null;
    }

    @Override
    public List<Entry> genEntryList() {
        int size = attributes == null ? 2 : attributes.length + 2;
        List<Entry> list = new ArrayList<>(size);
        list.add(new Entry("opNum", String.valueOf(opNum)));
        list.add(new Entry("state", String.valueOf(state)));
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                list.add(attributes[i]);
            }
        }
        return list;
    }

    @Override
    public void updateFromEntryList(List<Entry> entries) {
        opNum = Long.parseLong(entries.get(0).getValue());
        state = Integer.parseInt(entries.get(1).getValue());
        attributes = new Entry[entries.size() - 2];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = entries.get(i + 2);
        }
    }
}
