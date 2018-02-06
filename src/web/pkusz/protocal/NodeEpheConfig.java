package web.pkusz.protocal;

import web.pkusz.serialize.Entry;
import web.pkusz.serialize.Serialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 2018/1/6.
 */
public class NodeEpheConfig implements Serialize {

    int type;
    long commitOpNum;
    int state;

    public NodeEpheConfig(int type, long commitOpNum, int state) {
        this.type = type;
        this.commitOpNum = commitOpNum;
        this.state = state;
    }

    public NodeEpheConfig() {
        type = 0;
        commitOpNum = 0;
        state = 0;
    }

    public int getType() {
        return type;
    }

    public long getCommitOpNum() {
        return commitOpNum;
    }

    public int getState() {
        return state;
    }

    @Override
    public List<Entry> genEntryList() {
        List<Entry> list = new ArrayList<>(3);
        list.add(new Entry("type", String.valueOf(type)));
        list.add(new Entry("commitOpNum", String.valueOf(commitOpNum)));
        list.add(new Entry("state", String.valueOf(state)));
        return list;
    }

    @Override
    public void updateFromEntryList(List<Entry> entries) {
        type = Integer.parseInt(entries.get(0).getValue());
        commitOpNum = Long.parseLong(entries.get(1).getValue());
        state = Integer.parseInt(entries.get(2).getValue());
    }
}
